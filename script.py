from packaging.requirements import Requirement
from tqdm import tqdm
import requests
import json
import time

# Global configuration: fields to keep from the main "info" object
FIELDS_TO_KEEP = [
    "author",
    "author_email",
    "classifiers",
    "description",
    "project_urls",
    "requires_dist",
    "requires_python",
    "version",
    "releases"
]

# Fields to keep for each specific release
RELEASE_FIELDS_TO_KEEP = [
    "requires_dist",
    "requires_python"
]

visited = set()
not_visited_yet = {'Django'}

def parse_requires_dist_generic(requires_dist_list):
    """
    Parse a list of requires_dist entries without evaluating environment markers.
    Returns a list of dictionaries with detailed dependency information.
    """
    parsed_deps = []

    for entry in requires_dist_list:
        try:
            req = Requirement(entry)
            extra = None

            # Detect optional extras (e.g., extra == "argon2")
            if req.marker and "extra" in str(req.marker):
                extra = str(req.marker).split("extra ==")[-1].strip(" \"'")

            parsed_deps.append({
                "package_name": req.name,
                "version_specifier": str(req.specifier) if req.specifier else None,
                "marker": str(req.marker) if req.marker else None,
                "extra": extra
            })

        except Exception as e:
            print(f"Warning: could not parse '{entry}': {e}")

    return parsed_deps   

def find_package_info(package_name):
    """
    Fetch metadata for a given package from PyPI, filter selected fields,
    and for each release, fetch its individual metadata (also filtered).
    """
    url = f"https://pypi.org/pypi/{package_name}/json"

    try:
        response = requests.get(url, timeout=10)
        response.raise_for_status()
        json_data = response.json()

        info = json_data.get("info", {})
        releases = json_data.get("releases", {})

        requires_dist = info.get("requires_dist", [])

        for entry in (requires_dist or []):
            try:
                req = Requirement(entry)
                if req.name in visited:
                    continue
                if req.name in not_visited_yet:
                    continue
                not_visited_yet.add(req.name)
                
            except Exception as e:
                print(f"Warning: could not parse '{entry}': {e}")

        # Filter only the desired top-level fields
        filtered_info = {k: info[k] for k in FIELDS_TO_KEEP if k in info}

        # Prepare the releases container
        filtered_info["releases"] = {}

        # Iterate through all versions
        for version in tqdm(releases.keys(), desc=f"Processing {package_name} releases", unit="version"):
            version_url = f"https://pypi.org/pypi/{package_name}/{version}/json"
            try:
                v_response = requests.get(version_url, timeout=10)
                v_response.raise_for_status()
                version_json = v_response.json()

                # Extract the "info" section from the versioned JSON
                version_info = version_json.get("info", {})
                vulnerabilities = version_json.get("vulnerabilities", None)

                filtered_release_data = {}

                # SECOND FILTER: keep only fields listed in RELEASE_FIELDS_TO_KEEP
                for field in RELEASE_FIELDS_TO_KEEP:
                    if field in version_info:
                        filtered_release_data[field] = version_info[field]

                filtered_release_data["vulnerabilities"] = vulnerabilities

                filtered_info["releases"][version] = filtered_release_data

            except requests.RequestException:
                print(f"Warning: failed to retrieve {package_name} version {version}, skipping...")
                filtered_info["releases"][version] = {}

            # Avoid hitting PyPI rate limits
            time.sleep(0.5)

        return filtered_info

    except requests.exceptions.HTTPError as http_err:
        print(f"HTTP error occurred for package '{package_name}': {http_err}")
    except requests.exceptions.RequestException as req_err:
        print(f"Network error occurred for package '{package_name}': {req_err}")
    except ValueError as json_err:
        print(f"Failed to parse JSON for package '{package_name}': {json_err}")
    except Exception as err:
        print(f"Unexpected error occurred for package '{package_name}': {err}")


if __name__ == "__main__":
    print("Starting dataset creation...")
    dataset = {}
    while not_visited_yet:
        package = not_visited_yet.pop()
        if package in visited:
            continue

        dataset[package] = find_package_info(package)
        visited.add(package)

        print(f"Number of visited packages: {len(visited)}\nNumber of packages not yet visited: {len(not_visited_yet)}")

        if len(visited) % 2 == 0:
            with open("dataset.json", "w", encoding="utf-8") as f:
                json.dump(dataset, f, ensure_ascii=False, indent=4)