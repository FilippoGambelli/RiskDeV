import json
import os
import re
from neo4j import GraphDatabase
from packaging.specifiers import SpecifierSet
from packaging.version import Version as PyVersion, InvalidVersion
from tqdm import tqdm

# --- Configuration ---
FILES = {
    "packages": "pkg-cve/package.json",
    "vulnerabilities": "pkg-cve/vulnerability.json",
    "projects": "project-user/projects.json"
}

NEO4J_URI = "bolt://localhost:7687"
NEO4J_USER = "neo4j"
NEO4J_PASSWORD = "password"
TARGET_DB = "neo4j"

BATCH_SIZE = 2000
MAX_VERSION_LEN = 6

VERSION_WEIGHTS = {
    'dev': -10, 'a': -5, 'alpha': -5, 'b': -4, 'beta': -4,
    'rc': -1, 'c': -1, 'pre': -1, 'post': 1, 'pl': 1, 'r': 1, 'final': 0
}

# --- Helpers ---
def normalize_version(version_str):
    """
    Converts a version string into a list of integers/weights for comparison.
    Example: "1.0.0-beta" -> [1, 0, 0, -4, 0, 0]
    """
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
    "DEPENDS_ON": {}, 
    "USES": {},        
    "HAS_VERSION": {},
    "AFFECTED_BY": {}
}

class Neo4jImporter:
    def __init__(self, uri, user, password, database="neo4j"):
        self.driver = GraphDatabase.driver(uri, auth=(user, password))
        self.database = database
        self.available_versions_map = {} 
        self.raw_package_data = []

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
                # 1. Enforce Floats (Double in Java)
                if neo_key in ["risk_score", "baseScore"]:
                    try:
                        val = float(val)
                    except (ValueError, TypeError):
                        val = 0.0
                
                # 2. Enforce Strings (prevents "Cannot construct StringValue from null")
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

    def _load_json(self, filename):
        if not os.path.exists(filename):
            print(f"File not found: {filename}")
            return []
        try:
            with open(filename, "r", encoding="utf-8") as f:
                content = f.read().strip()
                if not content: return []
                try:
                    data = json.loads(content)
                    return [data] if isinstance(data, dict) else data
                except json.JSONDecodeError: return []
        except Exception as e:
            print(f"Error reading '{filename}': {e}")
            return []

    def _run_batch(self, query, data, description="Processing"):
        if not data: return
        chunks = [data[i:i + BATCH_SIZE] for i in range(0, len(data), BATCH_SIZE)]
        with self.driver.session(database=self.database) as session:
            for chunk in tqdm(chunks, desc=description, unit="batch"):
                session.run(query, batch=chunk)

    def _parse_requirement(self, req_input):
        req_str = ""
        # Handle Dictionary input (common in package.json)
        if isinstance(req_input, dict):
            req_str = req_input.get("full") or req_input.get("name")
        # Handle String input
        elif isinstance(req_input, str):
            req_str = req_input

        if not req_str:
            return None, None

        # Remove environment markers
        base_req = req_str.split(';')[0].strip()
        
        match = re.match(r"^([a-zA-Z0-9_\-\.]+)(.*)$", base_req)
        
        if not match: 
            return None, None
            
        name = match.group(1).strip()
        constraints = match.group(2).strip().replace("(", "").replace(")", "")
        
        return name, constraints

    def _resolve_dependencies(self):
        edges = []
        print("Resolving dependencies...")
        
        for item in tqdm(self.raw_package_data, desc="Building Graph"):
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
                    except Exception: 
                        continue

                for target in valid_targets:
                    edges.append({
                        "src_pkg": src_pkg_name,
                        "src_ver": src_ver_str,
                        "tgt_pkg": req_name,
                        "tgt_ver": target["version_str"],
                        "rel_props": rel_props
                    })
        return edges

    def create_constraints(self):
        print("Creating constraints...")
        queries = [
            "CREATE INDEX IF NOT EXISTS FOR (v:Version) ON (v.package_name, v.version)",
            "CREATE INDEX IF NOT EXISTS FOR (v:Version) ON (v.package_name)",
            "CREATE CONSTRAINT IF NOT EXISTS FOR (vn:Vulnerability) REQUIRE vn.cve_id IS UNIQUE",
            "CREATE CONSTRAINT IF NOT EXISTS FOR (prj:Project) REQUIRE prj.name IS UNIQUE"
        ]
        with self.driver.session(database=self.database) as session:
            for q in queries: session.run(q)

    def import_vulnerabilities_base(self):
        data = self._load_json(FILES["vulnerabilities"])
        batch = []
        for item in data:
            props = self._map_properties(item, "Vulnerability")
            if "cve_id" in props:
                batch.append(props)

        query = """
        UNWIND $batch AS row
        MERGE (vn:Vulnerability {cve_id: row.cve_id})
        SET vn += row
        """
        self._run_batch(query, batch, "Importing Base Vulnerabilities")

    def import_packages_and_versions(self):
        self.raw_package_data = self._load_json(FILES["packages"])
        batch_nodes = []
        
        for item in self.raw_package_data:
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
                
                # Calculate version_array immediately for imported nodes
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
        self._run_batch(query, batch_nodes, "Importing Packages & Versions")

    def import_dependencies(self):
        edges = self._resolve_dependencies()
        if not edges:
            print("WARNING: No dependencies found to import.")
        
        query = """
        UNWIND $batch AS row
        MATCH (s:Version {package_name: row.src_pkg, version: row.src_ver})
        MATCH (t:Version {package_name: row.tgt_pkg, version: row.tgt_ver})
        MERGE (s)-[r:DEPENDS_ON]->(t)
        SET r += row.rel_props
        """
        self._run_batch(query, edges, "Importing Dependencies")

    def import_projects(self):
        data = self._load_json(FILES["projects"])
        batch = []
        for item in data:
            props = self._map_properties(item, "Project")
            if not props.get("name"): continue
            
            dependencies = []
            for dep in item.get("packages", []):
                # FIX 1: Ensure name and version exist
                if not dep.get("name") or not dep.get("version"):
                    continue
                    
                rel_props = self._map_rel_properties(dep, "USES")
                
                dependencies.append({
                    "name": dep.get("name"), 
                    "version": dep.get("version"),
                    "rel_props": rel_props,
                    # FIX 2: Compute version array here to avoid nulls later
                    "version_array": normalize_version(dep.get("version"))
                })
            
            if dependencies: # Only append if dependencies exist
                batch.append({ "props": props, "deps": dependencies })

        # FIX 3: Updated Query with ON CREATE SET for robustness
        query = """
        UNWIND $batch AS row
        MERGE (prj:Project {name: row.props.name})
        SET prj += row.props
        FOREACH (dep IN row.deps |
            MERGE (p:Package {package_name: dep.name})
            
            MERGE (v:Version {package_name: dep.name, version: dep.version})
            ON CREATE SET 
                v.version_array = dep.version_array,
                v.risk_score = 0.0,
                v.documentation = '',
                v.requires_python = ''
            
            MERGE (p)-[:HAS_VERSION]->(v)
            MERGE (prj)-[r:USES]->(v)
            SET r += dep.rel_props
        )
        """
        self._run_batch(query, batch, "Importing Projects")

    def set_default_values(self):
        print("Sanitizing data types and setting defaults...")
        
        queries = [
            # 1. Force Strings (Handles NULLs and Type Mismatches)
            "MATCH (v:Version) SET v.documentation = toString(coalesce(v.documentation, ''))",
            "MATCH (v:Version) SET v.requires_python = toString(coalesce(v.requires_python, ''))",
            
            # 2. Force Floats (Java Double)
            "MATCH (v:Version) SET v.risk_score = toFloat(coalesce(v.risk_score, 0.0))",
            
            # 3. Vulnerability Strings
            "MATCH (vn:Vulnerability) SET vn.description = toString(coalesce(vn.description, ''))",
            
            # 4. Vulnerability Floats
            "MATCH (vn:Vulnerability) SET vn.baseScore = toFloat(coalesce(vn.baseScore, 0.0))"
        ]
        
        with self.driver.session(database=self.database) as session:
            for q in queries:
                session.run(q)

        # Recalculate version_array for any stragglers
        fetch_query = """
            MATCH (v:Version)
            WHERE v.version_array IS NULL
            RETURN v.package_name AS pkg, v.version AS ver
        """
        
        updates = []
        with self.driver.session(database=self.database) as session:
            result = session.run(fetch_query)
            for record in result:
                norm_ver = normalize_version(record["ver"])
                updates.append({
                    "pkg": record["pkg"],
                    "ver": record["ver"],
                    "version_array": norm_ver
                })

        if updates:
            update_query = """
                UNWIND $batch AS row
                MATCH (v:Version {package_name: row.pkg, version: row.ver})
                SET v.version_array = row.version_array
            """
            self._run_batch(update_query, updates, "Recalculating missing version_arrays")

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