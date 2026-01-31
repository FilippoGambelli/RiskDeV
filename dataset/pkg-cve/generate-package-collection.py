import json
import re

# --- CONFIGURATION ---
INPUT_FILE = "allInfoPackages.json"
VULN_DB_FILE = "vulnerability.json"
OUTPUT_FILE = "package.json"

# Fixed length for version comparison arrays (Major.Minor.Patch.PreType.PreNum.Dev)
MAX_VERSION_LEN = 6 

# Numeric weights for pre/post release tags to allow mathematical comparison
VERSION_WEIGHTS = {
    'dev': -4,
    'a': -3, 'alpha': -3,
    'b': -2, 'beta': -2,
    'rc': -1, 'c': -1, 'pre': -1,
    'post': 1, 'pl': 1,
}

VERSION_PATTERNS = {
    "version_gte": r">=\s*([0-9a-zA-Z\.\-_]+)",
    "version_lte": r"<=\s*([0-9a-zA-Z\.\-_]+)",
    "version_gt": r">\s*([0-9a-zA-Z\.\-_]+)",
    "version_lt": r"<\s*([0-9a-zA-Z\.\-_]+)",
    "version_eq": r"==\s*([0-9a-zA-Z\.\-_]+)",
    "version_neq": r"!=\s*([0-9a-zA-Z\.\-_]+)"
}

def parse_dependency(dep_string):
    """Extracts package name and version constraints from a PEP 508 dependency string."""
    result = {
        "full": dep_string,
        "name": None,
        "version_gte": None, "version_lte": None,
        "version_gt": None, "version_lt": None,
        "version_eq": None, "version_neq": None
    }
    if not dep_string:
        return result

    # Strip environment markers (parts after ;)
    main_part = dep_string.split(";", 1)[0].strip()

    # Match the package name at the start of the string
    name_match = re.match(r"^([a-zA-Z0-9\-_\.]+)", main_part)
    if name_match:
        result["name"] = name_match.group(1)

    # Extract specific version constraints using regex patterns
    for field, pattern in VERSION_PATTERNS.items():
        match = re.search(pattern, main_part)
        if match:
            result[field] = match.group(1)

    return result

def load_json(filename):
    try:
        with open(filename, 'r', encoding='utf-8') as f:
            return json.load(f)
    except (FileNotFoundError, json.JSONDecodeError) as e:
        print(f"Error loading {filename}: {e}")
        return None

def save_json(data, filename):
    try:
        with open(filename, 'w', encoding='utf-8') as f:
            json.dump(data, f, indent=4, ensure_ascii=False)
        print(f"Success: Created '{filename}' with {len(data)} entries.")
    except Exception as e:
        print(f"Error saving {filename}: {e}")

def normalize_version(version_str):
    """Converts a version string into a list of integers for easy sorting."""
    if not version_str:
        return [0] * MAX_VERSION_LEN

    v = str(version_str).lower()
    # Split into numeric and alphabetic chunks
    parts = re.findall(r'(\d+|[a-z]+)', v)
    
    normalized = []
    for part in parts:
        if part.isdigit():
            normalized.append(int(part))
        else:
            # Default to -5 for unknown tags to ensure they rank below known releases
            normalized.append(VERSION_WEIGHTS.get(part, -5))
    
    # Pad with zeros to maintain fixed length
    while len(normalized) < MAX_VERSION_LEN:
        normalized.append(0)
        
    return normalized[:MAX_VERSION_LEN]

def main():
    raw_packages = load_json(INPUT_FILE)
    vuln_db = load_json(VULN_DB_FILE)

    if raw_packages is None:
        return

    # Create a quick lookup for CVE scores
    vuln_map = {}
    if vuln_db:
        for item in vuln_db:
            cve = item.get('cve_id')
            score = item.get('metrics', {}).get('baseScore', 0.0)
            if cve:
                vuln_map[cve] = float(score)

    final_results = []

    for pkg in raw_packages:
        package_id = pkg.get("_id")
        
        for ver in pkg.get("versions_detailed", []):
            raw_vulns = ver.get("vulnerabilities") or []
            
            # Map CVE IDs to their numeric scores and find the highest risk
            scores = []
            seen_cves = set()
            for v in raw_vulns:
                cve_id = v.get('cve_id')
                if cve_id and cve_id not in seen_cves:
                    if cve_id in vuln_map:
                        scores.append(vuln_map[cve_id])
                    seen_cves.add(cve_id)

            max_risk_score = round(max(scores), 1) if scores else 0.0
            
            version_str = ver.get("version")
            
            entry = {
                "package_name": package_id,
                "version": version_str,
                "version_array": normalize_version(version_str),
                "author": pkg.get("author"),
                "author_email": pkg.get("author_email"),
                "description": pkg.get("description"),
                "package_url": pkg.get("package_url"),
                "documentation": pkg.get("Documentation"),
                "upload_time": ver.get("upload_time"),
                "requires_dist": [parse_dependency(dep) for dep in (ver.get("requires_dist") or [])],
                "requires_python": ver.get("requires_python"),
                "vulnerabilities": raw_vulns,
                "risk_score": max_risk_score
            }
            final_results.append(entry)

    save_json(final_results, OUTPUT_FILE)

if __name__ == "__main__":
    main()