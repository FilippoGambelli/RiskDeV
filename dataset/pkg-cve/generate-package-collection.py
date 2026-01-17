import json

# --- CONFIGURATION ---
INPUT_FILE = "allInfoPackages.json"
VULN_DB_FILE = "vulnerability.json"
OUTPUT_FILE = "package.json"

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
            json.dump(data, f, indent=4)
        print(f"Success: Created '{filename}' with {len(data)} entries.")
    except Exception as e:
        print(f"Error saving {filename}: {e}")

def main():
    raw_packages = load_json(INPUT_FILE)
    vuln_db = load_json(VULN_DB_FILE)

    if raw_packages is None:
        return

    # Build CVE lookup table for O(1) access time
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
            
            # Calculate Risk Score (Max Severity)
            scores = []
            seen_cves = set()
            for v in raw_vulns:
                cve_id = v.get('cve_id')
                if cve_id and cve_id not in seen_cves:
                    if cve_id in vuln_map:
                        scores.append(vuln_map[cve_id])
                    seen_cves.add(cve_id)

            max_risk_score = round(max(scores), 1) if scores else 0.0

            # Flatten and combine data
            entry = {
                "package_name": package_id,
                "version": ver.get("version"),
                "author": pkg.get("author"),
                "author_email": pkg.get("author_email"),
                "description": pkg.get("description"),
                "package_url": pkg.get("package_url"),
                "documentation": pkg.get("Documentation"),
                "upload_time": ver.get("upload_time"),
                "requires_dist": ver.get("requires_dist"),
                "requires_python": ver.get("requires_python"),
                "vulnerabilities": raw_vulns,
                "risk_score": max_risk_score
            }
            final_results.append(entry)

    save_json(final_results, OUTPUT_FILE)

if __name__ == "__main__":
    main()