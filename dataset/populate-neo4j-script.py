import json
import os
import re
from neo4j import GraphDatabase
from packaging.specifiers import SpecifierSet
from packaging.version import Version as PyVersion, InvalidVersion
from tqdm import tqdm

# ==========================================
# 1. Configuration & File Paths
# ==========================================
FILES = {
    "packages": "pkg-cve/package.json",
    "vulnerabilities": "pkg-cve/vulnerability.json",
    "projects": "project-user/projects.json",
    "users": "project-user/users.json"
}

NEO4J_URI = "bolt://localhost:7687"
NEO4J_USER = "neo4j"
NEO4J_PASSWORD = "password"
TARGET_DB = "neo4j"

BATCH_SIZE = 2000

# ==========================================
# 2. Node Property Mapping (FLESSIBILE)
# ==========================================
PROPERTY_MAPPING = {
    "Package": {
        "package_name": "package_name",
        # "ecosystem": "ecosystem"
    },
    "Version": {
        "package_name": "package_name",
        "version": "version",
        # "release_date": "upload_time"
    },
    "Vulnerability": {
        "cve_id": "cve_id",
        # "severity": "metrics.baseSeverity"
    },
    "Project": {
        "name": "name",
        # "created_at": "created_date"
    },
    "User": {
        "username": "username",
        # "email": "email"
    }
}

# ==========================================
# 3. Relationship Property Mapping (NUOVO)
# ==========================================
# Definisci qui le proprietà che vuoi sulle relazioni.
# La chiave è il TIPO di relazione (es. DEPENDS_ON).
# Il valore è il mapping "ProprietàNeo4j": "Path.Json"
# Nota: Il path JSON è relativo all'oggetto che sta generando la relazione.

RELATIONSHIP_PROPERTIES = {
    "DEPENDS_ON": {
        # "constraint": "constraint_string"  <-- Esempio (va popolato nella logica custom)
    },
    "USES": {
        # "usage_type": "type"               <-- Esempio: Dev vs Prod dependency
    },
    "WORKS_ON": {
        # "role": "role"                     <-- Esempio: Ruolo nel progetto (da array collaborators)
    },
    "HAS_VERSION": {
        # "is_latest": "is_latest"
    },
    "AFFECTED_BY": {
        # "note": "note"
    }
}

class Neo4jImporter:
    def __init__(self, uri, user, password, database="neo4j"):
        self.driver = GraphDatabase.driver(uri, auth=(user, password))
        self.database = database
        self.available_versions_map = {} 
        self.raw_package_data = []

    def close(self):
        self.driver.close()

    # ==========================================
    # Utility methods
    # ==========================================
    def _extract_value(self, data, path):
        keys = path.split('.')
        current = data
        try:
            for k in keys:
                current = current[k]
            return current
        except (KeyError, TypeError):
            return None

    def _map_properties(self, item, node_type):
        """Mappa proprietà dei Nodi"""
        mapping = PROPERTY_MAPPING.get(node_type, {})
        props = {}
        for neo_key, json_path in mapping.items():
            val = self._extract_value(item, json_path)
            if val is not None:
                props[neo_key] = val
        return props

    def _map_rel_properties(self, item, rel_type, extra_data=None):
        """
        Mappa proprietà delle Relazioni.
        item: l'oggetto JSON corrente (es. il singolo collaboratore o dipendenza).
        extra_data: dizionario opzionale per passare dati calcolati (es. stringa constraint).
        """
        mapping = RELATIONSHIP_PROPERTIES.get(rel_type, {})
        props = {}
        
        # 1. Mappatura diretta da JSON
        for neo_key, json_path in mapping.items():
            val = self._extract_value(item, json_path)
            if val is not None:
                props[neo_key] = val
        
        # 2. Merge con dati extra calcolati (se ci sono chiavi coincidenti, extra vince)
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

    # ==========================================
    # Dependency Logic
    # ==========================================
    def _parse_requirement(self, req_str):
        base_req = req_str.split(';')[0].strip()
        match = re.match(r"^([a-zA-Z0-9_\-\.]+)(.*)$", base_req)
        if not match: return None, None
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
                if req_name not in self.available_versions_map: continue

                # Prepara eventuali proprietà della relazione (es. constraint originale)
                # Qui passiamo 'item' che è l'intero pacchetto, ma potremmo non avere dati specifici per il singolo link
                # Se volessimo salvare il vincolo: rel_props = {"constraint": req_constraints}
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
                    edges.append({
                        "src_pkg": src_pkg_name,
                        "src_ver": src_ver_str,
                        "tgt_pkg": req_name,
                        "tgt_ver": target["version_str"],
                        "rel_props": rel_props # Proprietà da salvare sulla relazione
                    })
        return edges

    # ==========================================
    # Import Methods
    # ==========================================
    
    def create_constraints(self):
        print("Creating constraints (Natural Keys)...")
        queries = [
            "CREATE CONSTRAINT IF NOT EXISTS FOR (p:Package) REQUIRE p.package_name IS UNIQUE",
            "CREATE INDEX IF NOT EXISTS FOR (v:Version) ON (v.package_name, v.version)",
            "CREATE CONSTRAINT IF NOT EXISTS FOR (vn:Vulnerability) REQUIRE vn.cve_id IS UNIQUE",
            "CREATE CONSTRAINT IF NOT EXISTS FOR (prj:Project) REQUIRE prj.name IS UNIQUE",
            "CREATE CONSTRAINT IF NOT EXISTS FOR (u:User) REQUIRE u.username IS UNIQUE"
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
                
                # Relazione HAS_VERSION props
                has_ver_props = self._map_rel_properties(item, "HAS_VERSION")

                # Relazione AFFECTED_BY props e links
                vuln_data = []
                for v in item.get("vulnerabilities", []):
                    cve = v.get("cve_id")
                    if cve:
                        # Mappa proprietà relazione usando l'oggetto vulnerabilità specifico
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
        
        // Relazione con proprietà
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

            # Packages (Relazione USES)
            dependencies = []
            for dep in item.get("packages", []):
                # Passiamo l'oggetto 'dep' per estrarre proprietà specifiche della relazione (es. se c'è "type": "dev")
                rel_props = self._map_rel_properties(dep, "USES")
                dependencies.append({
                    "name": dep.get("name"), 
                    "version": dep.get("version"),
                    "rel_props": rel_props
                })

            # Collaborators (Relazione WORKS_ON)
            collaborators = []
            for collab in item.get("collaborators", []):
                # Passiamo l'oggetto 'collab' per estrarre proprietà (es. "role")
                rel_props = self._map_rel_properties(collab, "WORKS_ON")
                collaborators.append({
                    "username": collab.get("username"), 
                    "email": collab.get("email"),
                    "rel_props": rel_props
                })
            
            batch.append({
                "props": props,
                "deps": dependencies,
                "collabs": collaborators
            })

        query = """
        UNWIND $batch AS row
        MERGE (prj:Project {name: row.props.name})
        SET prj += row.props

        FOREACH (dep IN row.deps |
            MERGE (p:Package {package_name: dep.name})
            MERGE (p)-[:HAS_VERSION]->(v:Version {version: dep.version})
            MERGE (prj)-[r:USES]->(v)
            SET r += dep.rel_props
        )
        
        FOREACH (user_data IN row.collabs |
            MERGE (u:User {username: user_data.username})
            ON CREATE SET u.email = user_data.email
            MERGE (u)-[r:WORKS_ON]->(prj)
            SET r += user_data.rel_props
        )
        """
        self._run_batch(query, batch, "Importing Projects")

    def import_users(self):
        data = self._load_json(FILES["users"])
        batch = []
        for item in data:
            props = self._map_properties(item, "User")
            if props.get("username"):
                batch.append(props)
            
        query = """
        UNWIND $batch AS row
        MERGE (u:User {username: row.username})
        SET u += row
        """
        self._run_batch(query, batch, "Importing Users Details")

    def run(self):
        self.create_constraints()
        self.import_vulnerabilities_base()
        self.import_packages_and_versions()
        self.import_dependencies()
        self.import_projects()
        self.import_users()
        print("\nIMPORT COMPLETED SUCCESSFULLY")

if __name__ == "__main__":
    importer = Neo4jImporter(NEO4J_URI, NEO4J_USER, NEO4J_PASSWORD)
    try:
        importer.run()
    except Exception as e:
        print(f"\nCRITICAL ERROR: {e}")
    finally:
        importer.close()