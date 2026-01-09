import json
import os
import re
from neo4j import GraphDatabase
from packaging.specifiers import SpecifierSet
from packaging.version import Version as PyVersion, InvalidVersion
from tqdm import tqdm


# ==========================================
# Configuration
# ==========================================
FILES = {
    "packages": "pkg-cve/package.json",
    "versions": "pkg-cve/packageVersion.json",
    "vulnerabilities": "pkg-cve/vulnerability.json",
    "projects": "project/project.json",
    "users": "user/allInfoUsers.json",
}

NEO4J_URI = "bolt://localhost:7687"
NEO4J_USER = "neo4j"
NEO4J_PASSWORD = "password"
TARGET_DB = "neo4j"

BATCH_SIZE = 2000


# ==========================================
# Data Model Mapping
# ==========================================
NEO4J_DATA_MODEL = {
    "Package": {"_id": "id"},
    "Version": {"_id": "id", "package_name": "name", "version": "version"},
    "Vulnerability": {"_id": "id"},
    "Project": {"_id": "id"},
    "User": {"_id": "id"},
}


class Neo4jImporter:
    def __init__(self, uri, user, password, database="neo4j"):
        self.driver = GraphDatabase.driver(uri, auth=(user, password))
        self.database = database
        self.available_versions_map = {}
        self.raw_version_data = []

    def close(self):
        self.driver.close()

    # ==========================================
    # Utility methods
    # ==========================================
    def _load_json(self, filename):
        if not os.path.exists(filename):
            return []

        try:
            with open(filename, "r", encoding="utf-8") as f:
                content = f.read().strip()
                if not content:
                    return []

                data = json.loads(content)
                return [data] if isinstance(data, dict) else data

        except Exception as e:
            print(f"Error reading '{filename}': {e}")
            return []

    def _prepare_data(self, raw_list, node_type, rel_keys=None):
        if rel_keys is None:
            rel_keys = []

        mapping = NEO4J_DATA_MODEL.get(node_type, {})
        processed = []

        for item in raw_list:
            props = {
                neo_k: item[k]
                for k, neo_k in mapping.items()
                if k in item
            }

            rels = {k: item[k] for k in rel_keys if k in item}

            if "_id" in item:
                rels["origin_id"] = item["_id"]

            processed.append({"props": props, "rels": rels})

        return processed

    def _run_batch(self, query, data, description):
        if not data:
            return

        chunks = [data[i:i + BATCH_SIZE] for i in range(0, len(data), BATCH_SIZE)]

        with self.driver.session(database=self.database) as session:
            for chunk in chunks:
                session.run(query, batch=chunk)

    # ==========================================
    # Dependency resolution
    # ==========================================
    def _parse_requirement(self, req_str):
        match = re.match(r"^([a-zA-Z0-9_\-\.]+)(.*)$", req_str.split(";")[0].strip())
        if not match:
            return None, None

        name = match.group(1).strip()
        constraints = match.group(2).strip().replace("(", "").replace(")", "")
        return name, constraints

    def _resolve_dependencies(self):
        edges = []

        # Only show progress bar here
        for item in tqdm(self.raw_version_data, desc="Resolving dependencies", unit="ver"):
            src_id = item.get("_id")
            requirements = item.get("requires_dist")

            if not src_id or not requirements:
                continue

            for req in requirements:
                name, constraints = self._parse_requirement(req)

                if not name or name not in self.available_versions_map:
                    continue

                candidates = self.available_versions_map[name]

                if not constraints:
                    valid_targets = candidates
                else:
                    try:
                        spec = SpecifierSet(constraints)
                        valid_targets = [
                            t for t in candidates
                            if spec.contains(t["ver_obj"], prereleases=True)
                        ]
                    except Exception:
                        continue

                for target in valid_targets:
                    edges.append({
                        "source": src_id,
                        "target": target["id"],
                    })

        return edges

    # ==========================================
    # Schema creation
    # ==========================================
    def create_schema(self):
        print("Creating schema...")

        queries = [
            "CREATE CONSTRAINT IF NOT EXISTS FOR (p:Package) REQUIRE p.id IS UNIQUE",
            "CREATE CONSTRAINT IF NOT EXISTS FOR (v:Version) REQUIRE v.id IS UNIQUE",
            "CREATE CONSTRAINT IF NOT EXISTS FOR (vuln:Vulnerability) REQUIRE vuln.id IS UNIQUE",
            "CREATE CONSTRAINT IF NOT EXISTS FOR (prj:Project) REQUIRE prj.id IS UNIQUE",
            "CREATE CONSTRAINT IF NOT EXISTS FOR (u:User) REQUIRE u.id IS UNIQUE",
            "CREATE INDEX IF NOT EXISTS FOR (p:Package) ON (p.name)",
            "CREATE INDEX IF NOT EXISTS FOR (v:Version) ON (v.version)",
        ]

        with self.driver.session(database=self.database) as session:
            for q in queries:
                session.run(q)

    # ==========================================
    # Import nodes
    # ==========================================
    def import_vulnerabilities(self):
        data = self._load_json(FILES["vulnerabilities"])
        batch = self._prepare_data(data, "Vulnerability")

        query = """
        UNWIND $batch AS row
        MERGE (v:Vulnerability {id: row.props.id})
        SET v += row.props
        """

        self._run_batch(query, batch, "Importing vulnerabilities")

    def import_packages(self):
        data = self._load_json(FILES["packages"])
        batch = self._prepare_data(data, "Package")

        query = """
        UNWIND $batch AS row
        MERGE (p:Package {id: row.props.id})
        SET p += row.props
        """

        self._run_batch(query, batch, "Importing packages")

    def import_versions(self):
        self.raw_version_data = self._load_json(FILES["versions"])

        # Build version lookup cache
        for item in self.raw_version_data:
            pkg = item.get("package_name")
            ver = item.get("version")
            vid = item.get("_id")

            if not (pkg and ver and vid):
                continue

            self.available_versions_map.setdefault(pkg, [])

            try:
                self.available_versions_map[pkg].append({
                    "ver_obj": PyVersion(ver),
                    "id": vid,
                })
            except InvalidVersion:
                pass

        batch = self._prepare_data(
            self.raw_version_data,
            "Version",
            rel_keys=["package_name", "vulnerabilities"],
        )

        # Add semver components
        for item in batch:
            ver_str = item["props"].get("version", "0.0.0")
            try:
                v = PyVersion(ver_str).release
                item["props"]["major"] = v[0] if len(v) > 0 else 0
                item["props"]["minor"] = v[1] if len(v) > 1 else 0
                item["props"]["patch"] = v[2] if len(v) > 2 else 0
            except Exception:
                item["props"]["major"] = 0
                item["props"]["minor"] = 0
                item["props"]["patch"] = 0

            item["rels"]["vuln_list"] = item["rels"].get("vulnerabilities", [])

        query = """
        UNWIND $batch AS row
        MERGE (v:Version {id: row.props.id})
        SET v += row.props

        WITH v, row
        MATCH (p:Package {id: row.rels.package_name})
        MERGE (p)-[:HAS_VERSION]->(v)

        FOREACH (entry IN row.rels.vuln_list |
            MERGE (vn:Vulnerability {id: entry.cve_id})
            MERGE (v)-[:AFFECTED_BY]->(vn)
        )
        """

        self._run_batch(query, batch, "Importing versions")

    # ==========================================
    # Import relationships
    # ==========================================
    def import_dependencies(self):
        edges = self._resolve_dependencies()

        query = """
        UNWIND $batch AS row
        MATCH (s:Version {id: row.source})
        MATCH (t:Version {id: row.target})
        MERGE (s)-[:DEPENDS_ON]->(t)
        """

        # Progress bar ONLY here
        chunks = [edges[i:i + BATCH_SIZE] for i in range(0, len(edges), BATCH_SIZE)]

        with self.driver.session(database=self.database) as session:
            for chunk in tqdm(chunks, desc="Importing dependencies", unit="batch"):
                session.run(query, batch=chunk)

    def import_projects(self):
        data = self._load_json(FILES["projects"])
        batch = self._prepare_data(data, "Project", ["packages"])

        query = """
        UNWIND $batch AS row
        MERGE (prj:Project {id: row.props.id})
        SET prj += row.props

        WITH prj, row
        UNWIND row.rels.packages AS req
        MATCH (p:Package {id: req.name})-[:HAS_VERSION]->(v:Version {version: req.version})
        MERGE (prj)-[:USES]->(v)
        """

        self._run_batch(query, batch, "Importing projects")

    def import_users(self):
        data = self._load_json(FILES["users"])
        batch = self._prepare_data(data, "User", ["project_ids"])

        query = """
        UNWIND $batch AS row
        MERGE (u:User {id: row.props.id})
        SET u += row.props

        WITH u, row
        UNWIND row.rels.project_ids AS pid
        MATCH (prj:Project {id: pid})
        MERGE (u)-[:OWNS_PROJECT]->(prj)
        """

        self._run_batch(query, batch, "Importing users")

    # ==========================================
    # Orchestration
    # ==========================================
    def run(self):
        self.create_schema()

        self.import_vulnerabilities()
        self.import_packages()
        self.import_versions()

        self.import_dependencies()
        self.import_projects()
        self.import_users()

        print("IMPORT COMPLETED SUCCESSFULLY")


if __name__ == "__main__":
    importer = Neo4jImporter(NEO4J_URI, NEO4J_USER, NEO4J_PASSWORD)

    try:
        importer.run()
    except Exception as e:
        print(f"\nCRITICAL ERROR: {e}")
    finally:
        importer.close()