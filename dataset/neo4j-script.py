import json
import re
from neo4j import GraphDatabase
from tqdm import tqdm

# --- CONFIGURATION ---
# Neo4j Connection Details
NEO4J_URI = "bolt://localhost:7687"
NEO4J_AUTH = ("neo4j", "password")

# Input Files (Must match the output of the crawler script)
FILE_PACKAGES = "packages.json"
FILE_CVES = "vulnerabilities.json"

class Neo4jLoader:
    def __init__(self, uri, auth):
        self.driver = GraphDatabase.driver(uri, auth=auth)

    def close(self):
        self.driver.close()

    def create_constraints(self):
        """
        Creates constraints and indexes to ensure data integrity and speed up queries.
        """
        print("1. Setting up Constraints and Indexes...")
        with self.driver.session() as session:
            # 1. Unique ID for Package (Matches MongoDB _id / UUID)
            session.run("CREATE CONSTRAINT IF NOT EXISTS FOR (p:Package) REQUIRE p.id IS UNIQUE")
            
            # 2. Index for Package Name (Crucial for linking dependencies by name)
            session.run("CREATE INDEX IF NOT EXISTS FOR (p:Package) ON (p.name)")
            
            # 3. Unique ID for Vulnerability (Matches MongoDB _id / CVE-ID)
            session.run("CREATE CONSTRAINT IF NOT EXISTS FOR (v:Vulnerability) REQUIRE v.id IS UNIQUE")
            
            # 4. Unique UID for PackageVersion (Composite key: package_uuid + version)
            session.run("CREATE CONSTRAINT IF NOT EXISTS FOR (pv:PackageVersion) REQUIRE pv.uid IS UNIQUE")

    def load_vulnerabilities(self):
        """
        Loads the Vulnerabilities from JSON into Neo4j.
        Nodes: (:Vulnerability)
        """
        print("2. Loading Vulnerabilities...")
        try:
            with open(FILE_CVES, "r", encoding="utf-8") as f:
                cves_data = json.load(f)
        except FileNotFoundError:
            print(f"Error: {FILE_CVES} not found.")
            return

        query = """
        UNWIND $batch AS row
        MERGE (v:Vulnerability {id: row._id})
        SET v.published = row.published,
            v.description_snippet = substring(row.description, 0, 50) + "..." // Store only a snippet in graph
        """
        
        # Process in batches of 1000 to avoid memory issues
        batch_size = 1000
        with self.driver.session() as session:
            for i in tqdm(range(0, len(cves_data), batch_size), desc="Importing CVEs"):
                batch = cves_data[i:i + batch_size]
                session.run(query, batch=batch)

    def load_packages_and_versions(self):
        """
        Loads Packages and their specific Versions.
        Links Versions to Vulnerabilities.
        Nodes: (:Package), (:PackageVersion)
        Rels: [:HAS_VERSION], [:AFFECTED_BY]
        """
        print("3. Loading Packages and Versions...")
        try:
            with open(FILE_PACKAGES, "r", encoding="utf-8") as f:
                packages_data = json.load(f)
        except FileNotFoundError:
            print(f"Error: {FILE_PACKAGES} not found.")
            return

        # Query explanation:
        # 1. Create the Package Node using the UUID (_id)
        # 2. Iterate through versions
        # 3. Create PackageVersion with a composite unique ID (pkg_id + version)
        # 4. Link Package -> Version
        # 5. Link Version -> Vulnerability (using cve_id ref)
        query = """
        MERGE (p:Package {id: $pkg_id})
        SET p.name = $pkg_name,
            p.homepage = $pkg_homepage

        WITH p
        UNWIND $versions AS ver
        
        // Create Version Node
        MERGE (pv:PackageVersion {uid: $pkg_id + '_' + ver.version})
        SET pv.number = ver.version,
            pv.upload_time = ver.upload_time
        
        // Create Relationship: Package -> Version
        MERGE (p)-[:HAS_VERSION]->(pv)
        
        // Create Relationship: Version -> Vulnerability (if any)
        FOREACH (vuln IN ver.vulnerabilities | 
            MERGE (v:Vulnerability {id: vuln.cve_id})
            MERGE (pv)-[:AFFECTED_BY]->(v)
        )
        """

        with self.driver.session() as session:
            for pkg in tqdm(packages_data, desc="Importing Packages"):
                session.run(query, 
                            pkg_id=pkg["_id"],  # Using UUID from MongoDB
                            pkg_name=pkg["name"],
                            pkg_homepage=pkg.get("Homepage"),
                            versions=pkg["versions_detailed"])

        return packages_data # Return data for the next step (dependencies)

    @staticmethod
    def get_clean_package_name(dependency_string):
        """
        Parses 'requests (>=2.0)' -> 'requests'
        """
        if not dependency_string: return None
        match = re.match(r"^([A-Za-z0-9_\-\.]+)", dependency_string)
        if match:
            return match.group(1)
        return None

    def load_dependencies(self, packages_data):
        """
        Parses 'requires_dist' and creates relationships between specific versions 
        and the generic target packages.
        Rel: (:PackageVersion)-[:DEPENDS_ON]->(:Package)
        """
        print("4. Linking Dependencies (Graph Topology)...")
        
        query = """
        MATCH (pv:PackageVersion {uid: $ver_uid})
        MATCH (target:Package) 
        WHERE toLower(target.name) = toLower($target_name) // Case-insensitive match
        MERGE (pv)-[:DEPENDS_ON]->(target)
        """

        total_rels = 0
        with self.driver.session() as session:
            
            # Iterate through local data to parse strings
            for pkg in tqdm(packages_data, desc="Building Links"):
                source_uuid = pkg["_id"]
                
                for ver in pkg["versions_detailed"]:
                    # Reconstruct the UID to find the node in Neo4j
                    version_uid = f"{source_uuid}_{ver['version']}"
                    
                    raw_deps = ver.get("requires_dist")
                    if not raw_deps:
                        continue

                    for raw_dep in raw_deps:
                        clean_name = self.get_clean_package_name(raw_dep)
                        
                        if clean_name:
                            # Run the query to link nodes
                            session.run(query, ver_uid=version_uid, target_name=clean_name)
                            total_rels += 1
                            
        print(f"   Successfully created approx. {total_rels} dependency relationships.")

def main():
    loader = Neo4jLoader(NEO4J_URI, NEO4J_AUTH)
    
    try:
        loader.create_constraints()
        loader.load_vulnerabilities()
        
        # Load packages and keep the data in memory for dependency processing
        pkg_data = loader.load_packages_and_versions()
        
        if pkg_data:
            loader.load_dependencies(pkg_data)
            
        print("\n--- Import Complete! ---")
        print("You can now query specific packages using the '_id' field.")
        
    except Exception as e:
        print(f"Critical Error during import: {e}")
    finally:
        loader.close()

if __name__ == "__main__":
    main()