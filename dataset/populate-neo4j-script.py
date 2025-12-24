import json
import os
import re
from neo4j import GraphDatabase

# FILE CONFIGURATION
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


class Neo4jImporter:
    def __init__(self, uri, user, password):
        self.driver = GraphDatabase.driver(uri, auth=(user, password))

    def close(self):
        self.driver.close()

    def _load_json_file(self, filename):
        if not os.path.exists(filename):
            print(f"File '{filename}' not found.")
            return []
        with open(filename, "r", encoding="utf-8") as f:
            return json.load(f)

    def clear_database(self):
        """Delete all nodes and relationships."""
        print("Clearing database...")
        query = "MATCH (n) DETACH DELETE n"
        with self.driver.session() as session:
            session.run(query)
        print("Database cleared.")

    def create_constraints_and_indexes(self):
        queries = [
            # Unique constraints
            "CREATE CONSTRAINT IF NOT EXISTS FOR (p:Package) REQUIRE p.id IS UNIQUE",
            "CREATE CONSTRAINT IF NOT EXISTS FOR (v:Version) REQUIRE v.id IS UNIQUE",
            "CREATE CONSTRAINT IF NOT EXISTS FOR (vuln:Vulnerability) REQUIRE vuln.id IS UNIQUE",
            "CREATE CONSTRAINT IF NOT EXISTS FOR (prj:Project) REQUIRE prj.id IS UNIQUE",
            "CREATE CONSTRAINT IF NOT EXISTS FOR (u:User) REQUIRE u.id IS UNIQUE",

            # Performance indexes
            "CREATE INDEX IF NOT EXISTS FOR (p:Package) ON (p.name)",
            "CREATE INDEX IF NOT EXISTS FOR (v:Version) ON (v.version)"
        ]
        with self.driver.session() as session:
            for q in queries:
                session.run(q)
        print("Constraints and indexes ready.")

    def _parse_dependencies(self, requires_dist_list):
        parsed = []
        if not requires_dist_list:
            return parsed

        pattern = re.compile(r"^([a-zA-Z0-9_\-\.]+)(.*)$")

        for item in requires_dist_list:
            match = pattern.match(item)
            if match:
                pkg_name = match.group(1).strip()
                constraint = match.group(2).strip()
                parsed.append({
                    "name": pkg_name,
                    "constraint": constraint
                })
        return parsed

    def import_packages(self, filename):
        data = self._load_json_file(filename)
        if not data:
            return

        query = """
        UNWIND $batch AS row
        MERGE (p:Package {id: row._id})
        SET p.name = row.package_name,
            p.author = row.author,
            p.url = row.package_url
        """
        with self.driver.session() as session:
            session.run(query, batch=data)
        print(f"Packages loaded: {len(data)}")

    def import_vulnerabilities(self, filename):
        data = self._load_json_file(filename)
        if not data:
            return

        query = """
        UNWIND $batch AS row
        MERGE (v:Vulnerability {id: row._id})
        SET v.description = row.description,
            v.published = row.published
        """
        with self.driver.session() as session:
            session.run(query, batch=data)
        print(f"Vulnerabilities loaded: {len(data)}")

    def import_versions_advanced(self, filename):
        raw_data = self._load_json_file(filename)
        if not raw_data:
            return

        processed = []
        for item in raw_data:
            deps = self._parse_dependencies(item.get("requires_dist"))
            processed.append({
                "_id": item["_id"],
                "package_id": item["package_id"],
                "version": item["version"],
                "upload_time": item.get("upload_time"),
                "vulnerabilities": item.get("vulnerabilities", []),
                "dependencies": deps
            })

        query = """
        UNWIND $batch AS row
        MATCH (p:Package {id: row.package_id})
        MERGE (v:Version {id: row._id})
        SET v.version = row.version,
            v.upload_time = row.upload_time
        MERGE (p)-[:HAS_VERSION]->(v)

        FOREACH (vuln_data IN row.vulnerabilities |
            MERGE (vn:Vulnerability {id: vuln_data.cve_id})
            MERGE (v)-[:AFFECTED_BY]->(vn)
        )

        FOREACH (dep IN row.dependencies |
            MERGE (target_pkg:Package {name: dep.name})
            MERGE (v)-[r:DEPENDS_ON]->(target_pkg)
            SET r.constraint = dep.constraint
        )
        """
        with self.driver.session() as session:
            session.run(query, batch=processed)
        print(f"Versions (with dependencies) loaded: {len(processed)}")

    def import_projects(self, filename):
        data = self._load_json_file(filename)
        if not data:
            return

        create_projects = """
        UNWIND $batch AS row
        MERGE (prj:Project {id: row._id})
        SET prj.name = row.name,
            prj.last_update = row.last_update
        """

        link_versions = """
        UNWIND $batch AS row
        MATCH (prj:Project {id: row._id})
        UNWIND row.packages AS pkg
        MATCH (p:Package {name: pkg.name})-[:HAS_VERSION]->(v:Version {version: pkg.version})
        MERGE (prj)-[:USES]->(v)
        """

        with self.driver.session() as session:
            session.run(create_projects, batch=data)
            session.run(link_versions, batch=data)

        print(f"Projects loaded: {len(data)}")

    def import_users(self, filename):
        data = self._load_json_file(filename)
        if not data:
            return

        query = """
        UNWIND $batch AS row
        MERGE (u:User {id: row._id})
        SET u.username = row.username
        WITH u, row
        UNWIND row.project_ids AS pid
        MATCH (prj:Project {id: pid})
        MERGE (u)-[:OWNS_PROJECT]->(prj)
        """
        with self.driver.session() as session:
            session.run(query, batch=data)

        print(f"Users loaded: {len(data)}")

    def run(self):
        print("STARTING DATA IMPORT\n")

        self.clear_database()
        self.create_constraints_and_indexes()

        print("\n--- Importing data ---")
        self.import_vulnerabilities(FILES["vulnerabilities"])
        self.import_packages(FILES["packages"])
        self.import_versions_advanced(FILES["versions"])
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