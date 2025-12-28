import json
import os
import re
from neo4j import GraphDatabase

# ==========================================
# 1. FILE CONFIGURATION
# ==========================================
FILES = {
    "packages": "pkg-cve/generalPackages.json",
    "versions": "pkg-cve/packageVersions.json",
    "vulnerabilities": "pkg-cve/vulnerabilities.json",
    "projects": "project/projects.json",
    "users": "user/users.json"
}

NEO4J_URI = "bolt://localhost:7687"
NEO4J_USER = "neo4j"
NEO4J_PASSWORD = "password"

# ==========================================
# 2. DATA EXTRACTION CONFIGURATION
# ==========================================
NEO4J_DATA_MODEL = {
    "Package": {
        "_id": "id",
        # "author": "author",
        #"package_url": "url",
        # "summary": "summary"
    },
    "Version": {
        "_id": "id",
        # "version": "version",
        # "upload_time": "upload_time"
    },
    "Vulnerability": {
        "_id": "id",
        # "published": "published",
        # "description": "description"
    },
    "Project": {
        "_id": "id",
        # "name": "name",
        # "last_update": "last_update"
    },
    "User": {
        "_id": "id",
        # "username": "username",
        # "email": "email"
    }
}

class Neo4jImporter:
    def __init__(self, uri, user, password):
        self.driver = GraphDatabase.driver(uri, auth=(user, password))

    def close(self):
        self.driver.close()

    def _load_json_file(self, filename):
        """Loads JSON data safely. Handles empty files and errors."""
        if not os.path.exists(filename):
            print(f"Warning: File '{filename}' not found. Skipping.")
            return []
        
        try:
            with open(filename, "r", encoding="utf-8") as f:
                content = f.read().strip()
                if not content:
                    print(f"Warning: File '{filename}' is empty. Skipping.")
                    return []
                data = json.loads(content)
                if isinstance(data, dict): return [data]
                return data
        except json.JSONDecodeError as e:
            print(f"CRITICAL ERROR: Could not parse JSON in '{filename}'.")
            print(f"Details: {e}")
            return []
        except Exception as e:
            print(f"Error reading '{filename}': {e}")
            return []

    def _prepare_batch(self, raw_data_list, node_type, relational_keys=[]):
        """
        Prepares the batch by separating:
        1. 'props': Primitive values safe to save in Neo4j (from NEO4J_DATA_MODEL).
        2. 'rels': Complex data (lists/dicts) needed for relationship logic.
        """
        mapping = NEO4J_DATA_MODEL.get(node_type, {})
        processed_list = []

        for item in raw_data_list:
            # Separate properties
            node_props = {}
            for json_key, neo4j_prop in mapping.items():
                if json_key in item:
                    node_props[neo4j_prop] = item[json_key]

            # Separate relational data
            rel_data = {}
            # Always grab _id as origin_id for reference
            if "_id" in item: 
                rel_data["origin_id"] = item["_id"]
            
            # Grab specific keys needed for this node type
            for key in relational_keys:
                if key in item:
                    rel_data[key] = item[key]

            processed_list.append({
                "props": node_props,
                "rels": rel_data
            })
            
        return processed_list

    def _parse_dependencies(self, requires_dist_list):
        parsed = []
        if not requires_dist_list: return parsed
        pattern = re.compile(r"^([a-zA-Z0-9_\-\.]+)(.*)$")
        for item in requires_dist_list:
            clean_item = item.split(";")[0].strip()
            match = pattern.match(clean_item)
            if match:
                pkg_name = match.group(1).strip()
                constraint = match.group(2).strip().replace('(', '').replace(')', '')
                parsed.append({"name": pkg_name, "constraint": constraint})
        return parsed

    def clear_database(self):
        print("Clearing database...")
        with self.driver.session() as session:
            session.run("MATCH (n) DETACH DELETE n")
        print("Database cleared.")

    def create_constraints_and_indexes(self):
        print("Creating constraints...")
        queries = [
            "CREATE CONSTRAINT IF NOT EXISTS FOR (p:Package) REQUIRE p.id IS UNIQUE",
            "CREATE CONSTRAINT IF NOT EXISTS FOR (v:Version) REQUIRE v.id IS UNIQUE",
            "CREATE CONSTRAINT IF NOT EXISTS FOR (vuln:Vulnerability) REQUIRE vuln.id IS UNIQUE",
            "CREATE CONSTRAINT IF NOT EXISTS FOR (prj:Project) REQUIRE prj.id IS UNIQUE",
            "CREATE CONSTRAINT IF NOT EXISTS FOR (u:User) REQUIRE u.id IS UNIQUE",
            "CREATE INDEX IF NOT EXISTS FOR (p:Package) ON (p.name)", 
            "CREATE INDEX IF NOT EXISTS FOR (v:Version) ON (v.version)"
        ]
        with self.driver.session() as session:
            for q in queries: session.run(q)
        print("Constraints and indexes ready.")

    def import_packages(self, filename):
        raw_data = self._load_json_file(filename)
        if not raw_data: return

        # Packages usually don't need complex rel_data here
        batch_data = self._prepare_batch(raw_data, "Package")

        query = """
        UNWIND $batch AS row
        MERGE (p:Package {id: row.props.id})
        SET p += row.props
        """
        with self.driver.session() as session:
            session.run(query, batch=batch_data)
        print(f"Packages loaded: {len(batch_data)}")

    def import_vulnerabilities(self, filename):
        raw_data = self._load_json_file(filename)
        if not raw_data: return

        batch_data = self._prepare_batch(raw_data, "Vulnerability")

        query = """
        UNWIND $batch AS row
        MERGE (v:Vulnerability {id: row.props.id})
        SET v += row.props
        """
        with self.driver.session() as session:
            session.run(query, batch=batch_data)
        print(f"Vulnerabilities loaded: {len(batch_data)}")

    def import_versions(self, filename):
        raw_data = self._load_json_file(filename)
        if not raw_data: 
            print("Skipping Versions import due to missing/empty data.")
            return

        # Prepare batch with relational keys needed
        batch_data = self._prepare_batch(raw_data, "Version", 
                                         relational_keys=["package_name", "requires_dist", "vulnerabilities"])
        
        # Pre-parsing dependencies in Python to keep Cypher clean
        final_batch = []
        for item in batch_data:
            item["rels"]["parsed_deps"] = self._parse_dependencies(item["rels"].get("requires_dist", []))
            item["rels"]["vuln_list"] = item["rels"].get("vulnerabilities", [])
            final_batch.append(item)

        query = """
        UNWIND $batch AS row
        // 1. Create Node (Safe SET)
        MERGE (v:Version {id: row.props.id})
        SET v += row.props

        // 2. Link to Package
        WITH v, row
        MATCH (p:Package {id: row.rels.package_name})
        MERGE (p)-[:HAS_VERSION]->(v)

        // 3. Link to Vulnerabilities
        FOREACH (vuln_entry IN row.rels.vuln_list |
            MERGE (vn:Vulnerability {id: vuln_entry.cve_id})
            MERGE (v)-[:AFFECTED_BY]->(vn)
        )

        // 4. Link Dependencies
        FOREACH (dep IN row.rels.parsed_deps |
            MERGE (target_pkg:Package {id: dep.name})
            MERGE (v)-[r:DEPENDS_ON]->(target_pkg)
            SET r.constraint = dep.constraint
        )
        """
        with self.driver.session() as session:
            session.run(query, batch=final_batch)
        print(f"Versions loaded: {len(final_batch)}")

    def import_projects(self, filename):
        raw_data = self._load_json_file(filename)
        if not raw_data: return

        # 'packages' is the complex field causing the crash. We move it to 'rels'.
        batch_data = self._prepare_batch(raw_data, "Project", relational_keys=["packages"])

        query = """
        UNWIND $batch AS row
        MERGE (prj:Project {id: row.props.id})
        SET prj += row.props  
        // Note: row.rels.packages is NOT set as a property, preventing the error

        WITH prj, row
        UNWIND row.rels.packages AS pkg_req
        // Match: Package Name + Version Number
        MATCH (p:Package {id: pkg_req.name})-[:HAS_VERSION]->(v:Version {version: pkg_req.version})
        MERGE (prj)-[:USES]->(v)
        """
        with self.driver.session() as session:
            session.run(query, batch=batch_data)
        print(f"Projects loaded: {len(batch_data)}")

    def import_users(self, filename):
        raw_data = self._load_json_file(filename)
        if not raw_data: return

        batch_data = self._prepare_batch(raw_data, "User", relational_keys=["project_ids"])

        query = """
        UNWIND $batch AS row
        MERGE (u:User {id: row.props.id})
        SET u += row.props

        WITH u, row
        UNWIND row.rels.project_ids AS pid
        MATCH (prj:Project {id: pid})
        MERGE (u)-[:OWNS_PROJECT]->(prj)
        """
        with self.driver.session() as session:
            session.run(query, batch=batch_data)
        print(f"Users loaded: {len(batch_data)}")

    def run(self):
        print("STARTING DATA IMPORT\n")
        self.clear_database()
        self.create_constraints_and_indexes()

        print("\n--- Importing Nodes and Relationships ---")
        self.import_vulnerabilities(FILES["vulnerabilities"])
        self.import_packages(FILES["packages"])
        self.import_versions(FILES["versions"])
        self.import_projects(FILES["projects"])
        self.import_users(FILES["users"])

        print("\nAll operations completed successfully!")

if __name__ == "__main__":
    importer = Neo4jImporter(NEO4J_URI, NEO4J_USER, NEO4J_PASSWORD)
    try:
        importer.run()
    except Exception as e:
        print(f"\nCritical error: {e}")
    finally:
        importer.close()