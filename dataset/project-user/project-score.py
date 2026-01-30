import json

# --- CONFIGURATION ---
PROJECTS_FILE = 'projects.json'                     # Input: User projects definition
PACKAGES_SCORED_FILE = '../pkg-cve/package.json'    # Input: Database of scored packages
OUTPUT_FILE = 'projects.json'                       # Output: Updated projects file

def load_json(filename):
    """Load JSON data from file with basic error handling."""
    try:
        with open(filename, 'r', encoding='utf-8') as f:
            return json.load(f)
    except FileNotFoundError:
        print(f"Error: File {filename} not found.")
        return []

def save_json(data, filename):
    """Save data to JSON file."""
    with open(filename, 'w', encoding='utf-8') as f:
        json.dump(data, f, indent=4)
    print(f"File saved: {filename}")

def create_lookup_key(name, version):
    """Generates a unique key for package matching (case-insensitive name)."""
    return f"{name.lower().strip()}|{version.strip()}"

def main():
    projects = load_json(PROJECTS_FILE)
    scored_packages_list = load_json(PACKAGES_SCORED_FILE)

    if not projects:
        print("No projects to analyze.")
        return

    if not scored_packages_list:
        print("No scored package database found.")
        return

    # Build a lookup dictionary for O(1) access to package scores
    # Key: "name|version" -> Value: {score, count}
    package_info_map = {}
    for pkg in scored_packages_list:
        p_name = pkg.get('package_name')
        p_ver = pkg.get('version')
        
        if p_name and p_ver:
            key = create_lookup_key(p_name, p_ver)
            p_vulns = pkg.get('vulnerabilities', [])
            
            package_info_map[key] = {
                'score': float(pkg.get('risk_score', 0.0)),
                'count': len(p_vulns)
            }

    # Iterate through projects and update their dependencies with risk data
    for project in projects:
        project_packages = project.get('packages', [])

        for item in project_packages:
            pkg_name = item.get('name')
            pkg_ver = item.get('version')

            # Default to safe values
            assigned_score = 0.0
            assigned_count = 0

            if pkg_name and pkg_ver:
                key = create_lookup_key(pkg_name, pkg_ver)
                if key in package_info_map:
                    info = package_info_map[key]
                    assigned_score = info['score']
                    assigned_count = info['count']

            # Inject calculated risk data into the project entry
            item['risk_score'] = assigned_score
            item['vulnerabilities_count'] = assigned_count

    save_json(projects, OUTPUT_FILE)

if __name__ == "__main__":
    main()