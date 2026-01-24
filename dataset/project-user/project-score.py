import json


PROJECTS_FILE = 'projects.json'                     # Input projects file
PACKAGES_SCORED_FILE = '../pkg-cve/package.json'    # Input scored packages file
OUTPUT_FILE = 'projects.json'                       # Output file (overwrites input projects)


def load_json(filename):
    """
    Load a JSON file and return the data.
    If file is not found, print an error and return an empty list.
    """
    try:
        with open(filename, 'r', encoding='utf-8') as f:
            return json.load(f)
    except FileNotFoundError:
        print(f"Error: File {filename} not found.")
        return []

def save_json(data, filename):
    """
    Save a Python object as JSON to the specified filename.
    """
    with open(filename, 'w', encoding='utf-8') as f:
        json.dump(data, f, indent=4)
    print(f"File saved: {filename}")

def create_lookup_key(name, version):
    """
    Create a standardized key for package lookup using lowercase name and version.
    """
    return f"{name.lower().strip()}|{version.strip()}"


def main():
    # Load projects and scored package database
    projects = load_json(PROJECTS_FILE)
    scored_packages_list = load_json(PACKAGES_SCORED_FILE)

    if not projects:
        print("No projects to analyze.")
        return

    if not scored_packages_list:
        print("No scored package database found.")
        return

    # Map each package|version to its risk score and vulnerability count
    package_info_map = {}
    for pkg in scored_packages_list:
        p_name = pkg.get('package_name')
        p_ver = pkg.get('version')
        p_score = pkg.get('risk_score', 0.0)
        p_vulns = pkg.get('vulnerabilities', [])

        vuln_count = len(p_vulns) if p_vulns else 0

        if p_name and p_ver:
            key = create_lookup_key(p_name, p_ver)
            package_info_map[key] = {
                'score': float(p_score),
                'count': vuln_count
            }

    for project in projects:
        project_packages = project.get('packages', [])

        for item in project_packages:
            pkg_name = item.get('name')
            pkg_ver = item.get('version')

            # Default values if package not found in scored database
            assigned_score = 0.0
            assigned_count = 0

            if pkg_name and pkg_ver:
                key = create_lookup_key(pkg_name, pkg_ver)
                if key in package_info_map:
                    info = package_info_map[key]
                    assigned_score = info['score']
                    assigned_count = info['count']

            # Update package info in the project
            item['risk_score'] = assigned_score
            item['vulnerabilities_count'] = assigned_count

    save_json(projects, OUTPUT_FILE)


if __name__ == "__main__":
    main()