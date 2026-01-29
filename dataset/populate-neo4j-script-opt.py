import ijson
import os
import re
from neo4j import GraphDatabase
from packaging.specifiers import SpecifierSet
from packaging.version import Version as PyVersion, InvalidVersion
from tqdm import tqdm

# --- Configuration ---
FILES = {
    "packages": "dataset/package.json",
    "vulnerabilities": "dataset/vulnerability.json",
    "projects": "dataset/user/projects.json"
}

NEO4J_URI = "bolt://localhost:7687"
NEO4J_USER = "neo4j"
NEO4J_PASSWORD = "password"
TARGET_DB = "neo4j"

BATCH_SIZE = 500
MAX_VERSION_LEN = 6

VERSION_WEIGHTS = {
    'dev': -10, 'a': -5, 'alpha': -5, 'b': -4, 'beta': -4,
    'rc': -1, 'c': -1, 'pre': -1, 'post': 1, 'pl': 1, 'r': 1, 'final': 0
}

# --- Helpers ---
def normalize_version(version_str):
    if not version_str:
        return [0] * MAX_VERSION_LEN
    v = str(version_str).lower()
    parts = re.findall(r'(\d+|[a-z]+)', v)
    normalized = []
    for part in parts:
        if part.isdigit():
            normalized.append(int(part))
        else:
            normalized.append(VERSION_WEIGHTS.get(part, -5))
    while len(normalized) < MAX_VERSION_LEN:
        normalized.append(0)
    return normalized[:MAX_VERSION_LEN]

# --- Mappings ---
PROPERTY_MAPPING = {
    "Package": { "package_name": "package_name" },
    "Version": {
        "package_name": "package_name",
        "version": "version",
        "risk_score": "risk_score",
        "requires_python": "requires_python",
        "documentation": "documentation"
    },
    "Vulnerability": {
        "cve_id": "cve_id",
        "description": "description",
        "baseScore": "metrics.baseScore"
    },
    "Project": { "name": "name" }
}

RELATIONSHIP_PROPERTIES = {
    "DEPENDS_ON": {}, "USES": {}, "HAS_VERSION": {}, "AFFECTED_BY": {}
}

class Neo4jImporter:
    def __init__(self, uri, user, password, database="neo4j"):
        self.driver = GraphDatabase.driver(uri, auth=(user, password))
        self.database = database
        self.available_versions_map = {}

    def close(self):
        self.driver.close()

    def _extract_value(self, data, path):
        if not isinstance(path, str): return None
        keys = path.split('.')
        current = data
        try:
            for k in keys:
                current = current[k]
            return current
        except (KeyError, TypeError):
            return None

    def _map_properties(self, item, node_type):
        mapping = PROPERTY_MAPPING.get(node_type, {})
        props = {}
        for neo_key, json_path in mapping.items():
            val = self._extract_value(item, json_path)
            if val is not None:
                if neo_key in ["risk_score", "baseScore"]:
                    try: val = float(val)
                    except: val = 0.0
                elif neo_key in ["documentation", "requires_python", "description", "package_name", "version", "cve_id"]:
                    val = str(val)
                props[neo_key] = val
        return props

    def _map_rel_properties(self, item, rel_type, extra_data=None):
        mapping = RELATIONSHIP_PROPERTIES.get(rel_type, {})
        props = {}
        for neo_key, json_path in mapping.items():
            val = self._extract_value(item, json_path)
            if val is not None:
                props[neo_key] = val
        if extra_data:
            props.update(extra_data)
        return props

    def _stream_json(self, filename):
        if not os.path.exists(filename):
            print(f"File not found: {filename}")
            return
        try:
            with open(filename, "rb") as f:
                for item in ijson.items(f, 'item'):
                    yield item
        except Exception as e:
            print(f"Error streaming '{filename}': {e}")

    def _run_batch(self, query, data, description="Processing"):
        if not data: return
        with self.driver.session(database=self.database) as session:
            session.run(query, batch=data)

    def _parse_requirement(self, req_input):
        req_str = ""
        if isinstance(req_input, dict):
            req_str = req_input.get("full") or req_input.get("name")
        elif isinstance(req_input, str):
            req_str = req_input
        if not req_str: return None, None
        base_req = req_str.split(';')[0].strip()
        match = re.match(r"^([a-zA-Z0-9_\-\.]+)(.*)$", base_req)
        if not match: return None, None
        name = match.group(1).strip()
        constraints = match.group(2).strip().replace("(", "").replace(")", "")
        return name, constraints

    def import_dependencies(self):
        print("Resolving dependencies (Streaming Pass)...")

        batch_edges = []
        query = """
        UNWIND $batch AS row
        MATCH (s:Version {package_name: row.src_pkg, version: row.src_ver})
        MATCH (t:Version {package_name: row.tgt_pkg, version: row.tgt_ver})
        MERGE (s)-[r:DEPENDS_ON]->(t)
        SET r += row.rel_props
        """

        for item in self._stream_json(FILES["packages"]):
            src_pkg_name = item.get("package_name")
            src_ver_str = item.get("version")
            requirements = item.get("requires_dist")

            if not src_pkg_name or not src_ver_str or not requirements:
                continue

            for req in requirements:
                req_name, req_constraints = self._parse_requirement(req)

                if not req_name or req_name not in self.available_versions_map:
                    continue

                rel_props = self._map_rel_properties(item, "DEPENDS_ON", extra_data={"constraint": req_constraints})
                candidates = self.available_versions_map[req_name]
                valid_targets = []

                if not req_constraints:
                    valid_targets = candidates
                else:
                    try:
                        spec = SpecifierSet(req_constraints)
                        valid_targets = [c for c in candidates if spec.contains(c["ver_obj"], prereleases=True)]
                    except Exception: continue

                for target in valid_targets:
                    batch_edges.append({
                        "src_pkg": src_pkg_name,
                        "src_ver": src_ver_str,
                        "tgt_pkg": req_name,
                        "tgt_ver": target["version_str"],
                        "rel_props": rel_props
                    })

            if len(batch_edges) >= BATCH_SIZE:
                self._run_batch(query, batch_edges, "Imp. Dependencies")
                batch_edges = []

        if batch_edges:
            self._run_batch(query, batch_edges, "Imp. Dependencies (Final)")

    def create_constraints(self):
        print("Creating constraints...")
        queries = [
            "CREATE INDEX IF NOT EXISTS FOR (v:Version) ON (v.package_name, v.version)",
            "CREATE INDEX IF NOT EXISTS FOR (v:Version) ON (v.package_name)",
            "CREATE CONSTRAINT IF NOT EXISTS FOR (vn:Vulnerability) REQUIRE vn.cve_id IS UNIQUE",
            "CREATE CONSTRAINT IF NOT EXISTS FOR (prj:Project) REQUIRE prj.name IS UNIQUE",
            "CREATE CONSTRAINT IF NOT EXISTS FOR (p:Package) REQUIRE p.package_name IS UNIQUE"
        ]
        with self.driver.session(database=self.database) as session:
            for q in queries: session.run(q)

    def import_vulnerabilities_base(self):
        batch = []
        query = """
        UNWIND $batch AS row
        MERGE (vn:Vulnerability {cve_id: row.cve_id})
        SET vn += row
        """
        for item in tqdm(self._stream_json(FILES["vulnerabilities"]), desc="Vulns"):
            props = self._map_properties(item, "Vulnerability")
            if "cve_id" in props:
                batch.append(props)

            if len(batch) >= BATCH_SIZE:
                self._run_batch(query, batch)
                batch = []
        if batch: self._run_batch(query, batch)

    def import_packages_and_versions(self):
        print("Importing Packages & Versions...")
        batch_nodes = []

        query = """
        UNWIND $batch AS row
        MERGE (p:Package {package_name: row.pkg.package_name})
        SET p += row.pkg
        MERGE (v:Version {package_name: row.ver.package_name, version: row.ver.version})
        SET v += row.ver
        MERGE (p)-[r:HAS_VERSION]->(v)
        SET r += row.has_ver_props
        FOREACH (v_data IN row.vulns |
            MERGE (vn:Vulnerability {cve_id: v_data.cve})
            MERGE (v)-[rel:AFFECTED_BY]->(vn)
            SET rel += v_data.rel_props
        )
        """

        for item in tqdm(self._stream_json(FILES["packages"]), desc="Pkgs"):
            pkg_name = item.get("package_name")
            ver_str = item.get("version")

            if pkg_name and ver_str:
                self.available_versions_map.setdefault(pkg_name, [])
                try:
                    self.available_versions_map[pkg_name].append({
                        "ver_obj": PyVersion(ver_str),
                        "version_str": ver_str
                    })
                except InvalidVersion: pass

                pkg_props = self._map_properties(item, "Package")
                ver_props = self._map_properties(item, "Version")
                ver_props["version_array"] = normalize_version(ver_str)
                has_ver_props = self._map_rel_properties(item, "HAS_VERSION")

                vuln_data = []
                for v in item.get("vulnerabilities", []):
                    cve = v.get("cve_id")
                    if cve:
                        rel_props = self._map_rel_properties(v, "AFFECTED_BY")
                        vuln_data.append({"cve": cve, "rel_props": rel_props})

                batch_nodes.append({
                    "pkg": pkg_props,
                    "ver": ver_props,
                    "has_ver_props": has_ver_props,
                    "vulns": vuln_data
                })

            if len(batch_nodes) >= BATCH_SIZE:
                self._run_batch(query, batch_nodes)
                batch_nodes = []

        if batch_nodes: self._run_batch(query, batch_nodes)

    def import_projects(self):
        batch = []
        query = """
        UNWIND $batch AS row
        MERGE (prj:Project {name: row.props.name})
        SET prj += row.props
        FOREACH (dep IN row.deps |
            MERGE (p:Package {package_name: dep.name})
            MERGE (v:Version {package_name: dep.name, version: dep.version})
            ON CREATE SET
                v.version_array = dep.version_array,
                v.risk_score = 0.0
            MERGE (p)-[:HAS_VERSION]->(v)
            MERGE (prj)-[r:USES]->(v)
            SET r += dep.rel_props
        )
        """
        for item in tqdm(self._stream_json(FILES["projects"]), desc="Projects"):
            props = self._map_properties(item, "Project")
            if not props.get("name"): continue

            dependencies = []
            for dep in item.get("packages", []):
                if not dep.get("name") or not dep.get("version"): continue
                rel_props = self._map_rel_properties(dep, "USES")
                dependencies.append({
                    "name": dep.get("name"),
                    "version": dep.get("version"),
                    "rel_props": rel_props,
                    "version_array": normalize_version(dep.get("version"))
                })

            if dependencies:
                batch.append({ "props": props, "deps": dependencies })

            if len(batch) >= BATCH_SIZE:
                self._run_batch(query, batch)
                batch = []

        if batch: self._run_batch(query, batch)

    def set_default_values(self):
        print("Sanitizing data types...")
        queries = [
            "MATCH (v:Version) WHERE v.risk_score IS NULL SET v.risk_score = 0.0",
            "MATCH (vn:Vulnerability) WHERE vn.baseScore IS NULL SET vn.baseScore = 0.0"
        ]
        with self.driver.session(database=self.database) as session:
            for q in queries: session.run(q)

    def run(self):
        self.create_constraints()
        self.import_vulnerabilities_base()
        self.import_packages_and_versions()
        self.import_dependencies() 
        self.import_projects()
        self.set_default_values()
        print("\nIMPORT COMPLETED SUCCESSFULLY")

if __name__ == "__main__":
    importer = Neo4jImporter(NEO4J_URI, NEO4J_USER, NEO4J_PASSWORD)
    try:
        importer.run()
    except Exception as e:
        print(f"\nCRITICAL ERROR: {e}")
        import traceback
        traceback.print_exc()
    finally:
        importer.close()
