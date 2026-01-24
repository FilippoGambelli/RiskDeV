import json
import re

# --- CONFIGURATION ---
INPUT_FILE = "allInfoPackages.json"
VULN_DB_FILE = "vulnerability.json"
OUTPUT_FILE = "package.json"

# Configuration for version normalization
MAX_VERSION_LEN = 6  # Fixed length of version array (Major.Minor.Patch.PreType.PreNum.Dev)

# Mapping for converting strings (alpha, beta, etc.) into numbers for sorting
# Negative values < 0 (pre-release) < 0 (final release) < Positive values (post release)
VERSION_WEIGHTS = {
    'dev': -4,
    'a': -3, 'alpha': -3,
    'b': -2, 'beta': -2,
    'rc': -1, 'c': -1, 'pre': -1,
    'post': 1, 'pl': 1,  # Patch level / post release
}

# --- Dependency Parsing ---
VERSION_PATTERNS = {
    "version_gte": r">=\s*([0-9a-zA-Z\.\-_]+)",
    "version_lte": r"<=\s*([0-9a-zA-Z\.\-_]+)",
    "version_gt": r">\s*([0-9a-zA-Z\.\-_]+)",
    "version_lt": r"<\s*([0-9a-zA-Z\.\-_]+)",
    "version_eq": r"==\s*([0-9a-zA-Z\.\-_]+)",
    "version_neq": r"!=\s*([0-9a-zA-Z\.\-_]+)"
}

def parse_dependency(dep_string):
    result = {
        "name": None,
        "version_gte": None,
        "version_lte": None,
        "version_gt": None,
        "version_lt": None,
        "version_eq": None,
        "version_neq": None
    }
    if not dep_string:
        return result

    # Prendi solo la parte prima del punto e virgola
    main_part = dep_string.split(";", 1)[0].strip()

    # Estrai il nome del pacchetto
    name_match = re.match(r"^([a-zA-Z0-9\-_\.]+)", main_part)
    if name_match:
        result["name"] = name_match.group(1)

    # Estrai le versioni
    for field, pattern in VERSION_PATTERNS.items():
        match = re.search(pattern, main_part)
        if match:
            result[field] = match.group(1)

    return result

# --- JSON Utilities ---
def load_json(filename):
    try:
        with open(filename, 'r', encoding='utf-8') as f:
            return json.load(f)
    except FileNotFoundError:
        print(f"Error: '{filename}' not found.")
        return None
    except json.JSONDecodeError:
        print(f"Error: '{filename}' is not a valid JSON.")
        return None

def save_json(data, filename):
    try:
        with open(filename, 'w', encoding='utf-8') as f:
            json.dump(data, f, indent=4, ensure_ascii=False)
        print(f"Success: Created '{filename}' with {len(data)} entries.")
    except Exception as e:
        print(f"Error saving {filename}: {e}")

# --- Version Normalization ---
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
            weight = VERSION_WEIGHTS.get(part, -5)
            normalized.append(weight)
    
    while len(normalized) < MAX_VERSION_LEN:
        normalized.append(0)
        
    return normalized[:MAX_VERSION_LEN]

# --- Main Processing ---
def main():
    raw_packages = load_json(INPUT_FILE)
    vuln_db = load_json(VULN_DB_FILE)

    if raw_packages is None:
        return

    # Build CVE lookup table
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
            
            # Compute risk score (max of CVE scores)
            scores = []
            seen_cves = set()
            for v in raw_vulns:
                cve_id = v.get('cve_id')
                if cve_id and cve_id not in seen_cves:
                    if cve_id in vuln_map:
                        scores.append(vuln_map[cve_id])
                    seen_cves.add(cve_id)

            max_risk_score = round(max(scores), 1) if scores else 0.0

            # --- VERSION NORMALIZATION ---
            version_str = ver.get("version")
            version_array = normalize_version(version_str)

            # --- DEPENDENCY PARSING ---
            requires = ver.get("requires_dist") or []
            structured_requires = [parse_dependency(dep) for dep in requires]

            entry = {
                "package_name": package_id,
                "version": version_str,
                "version_array": version_array,
                "author": pkg.get("author"),
                "author_email": pkg.get("author_email"),
                "description": pkg.get("description"),
                "package_url": pkg.get("package_url"),
                "documentation": pkg.get("Documentation"),
                "upload_time": ver.get("upload_time"),
                "requires_dist": structured_requires,
                "requires_python": ver.get("requires_python"),
                "vulnerabilities": raw_vulns,
                "risk_score": max_risk_score
            }
            final_results.append(entry)

    save_json(final_results, OUTPUT_FILE)

if __name__ == "__main__":
    main()