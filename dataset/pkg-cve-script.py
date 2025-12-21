import requests
import json
import re
import time
import uuid
from tqdm import tqdm

# --- Configuration ---
INITIAL_PACKAGES = [
    "requests",         # simple HTTP client
    "flask",            # micro web framework
    "django",           # full-featured web framework
    "numpy",            # numerical computing
    "pandas",           # data analysis
    "pytest",           # testing framework
    "python-dotenv",    # environment variable management
    "pillow",           # image processing
    "tqdm",             # progress bars in terminal
    "rich",             # colored and formatted output
    "matplotlib"        # data visualization
]
OUTPUT_FILE_PACKAGES = "packages.json"
OUTPUT_FILE_CVES = "vulnerabilities.json"

# --- NVD API CONFIGURATION ---
NVD_API_KEY = "dd504d2e-72e2-4bd4-ab26-ba05e997f07b"
NVD_SLEEP_TIME = 0.6

def get_clean_package_name(dependency_string):
    """
    Extracts package name from requirement string.
    Example: 'requests (>=2.0)' -> 'requests'
    """
    match = re.match(r"^([A-Za-z0-9_\-\.]+)", dependency_string)
    if match:
        return match.group(1)
    return None

def save_checkpoint(tree_data_dict, cve_data_dict):
    """
    Saves the current state to JSON files formatted for DocumentDB/Neo4j import.
    """
    try:
        # Convert Dictionary to List
        packages_list = list(tree_data_dict.values())
        
        with open(OUTPUT_FILE_PACKAGES, "w", encoding="utf-8") as f:
            json.dump(packages_list, f, indent=4)
        
        # Convert CVE Dictionary to List
        cves_list = []
        for cve_id, details in cve_data_dict.items():
            cve_doc = details.copy()
            cve_doc["_id"] = cve_id 
            cves_list.append(cve_doc)

        with open(OUTPUT_FILE_CVES, "w", encoding="utf-8") as f:
            json.dump(cves_list, f, indent=4)
            
        tqdm.write(f"   [Auto-Save] Data saved successfully.")
    except Exception as e:
        tqdm.write(f"   [Auto-Save Error] Could not save data: {e}")

def fetch_nvd_cve_details(cve_id, cve_database):
    """
    Fetches details from NIST NVD API for a specific CVE.
    """
    if cve_id in cve_database:
        return

    url = f"https://services.nvd.nist.gov/rest/json/cves/2.0?cveId={cve_id}"
    
    headers = {"User-Agent": "PythonScript/1.0"}
    if NVD_API_KEY:
        headers["apiKey"] = NVD_API_KEY

    try:
        tqdm.write(f"   [NVD] Fetching details for {cve_id}...")
        response = requests.get(url, headers=headers)
        
        if response.status_code == 200:
            data = response.json()
            vulnerabilities = data.get("vulnerabilities", [])
            
            if vulnerabilities:
                cve_item = vulnerabilities[0].get("cve", {})
                
                # Flatten description
                raw_descriptions = cve_item.get("descriptions", [])
                description_text = "No description available"
                for d in raw_descriptions:
                    if d.get("lang") == "en":
                        description_text = d.get("value")
                        break 

                cve_database[cve_id] = {
                    "published": cve_item.get("published"),
                    "description": description_text, 
                    "metrics": cve_item.get("metrics") 
                }
            else:
                cve_database[cve_id] = {
                    "error": "No data found in NVD", 
                    "description": "Not found"
                }

        elif response.status_code == 403:
             tqdm.write(f"   [NVD] 403 Forbidden. Check your API Key!")
             time.sleep(5)
        elif response.status_code == 429:
            tqdm.write(f"   [NVD] Rate Limit Hit! Waiting longer...")
            time.sleep(10)
        else:
            tqdm.write(f"   [NVD] Error {response.status_code} for {cve_id}")

        time.sleep(NVD_SLEEP_TIME)

    except Exception as e:
        tqdm.write(f"   [NVD] Exception for {cve_id}: {e}")

def fetch_package_data(package_name, visited_set, package_queue, cve_database):
    """
    Fetches package data, generates UUID, and processes versions.
    """
    norm_name = package_name.lower()
    if norm_name in visited_set:
        return None
    
    visited_set.add(norm_name)
    main_url = f"https://pypi.org/pypi/{package_name}/json"
    
    try:
        response = requests.get(main_url)
        if response.status_code != 200:
            tqdm.write(f"Warning: Failed to fetch {package_name} (HTTP {response.status_code})")
            return None
            
        data = response.json()
        info = data.get("info", {})
        project_urls = info.get("project_urls") or {}
        
        package_uuid = str(uuid.uuid4())
        
        package_entry = {
            "_id": package_uuid,
            "name": info.get("name"),
            "author": info.get("author"),
            "author_email": info.get("author_email"),
            "description": info.get("description"), 
            "package_url": info.get("package_url"),
            "summary": info.get("summary"),
            "Documentation": project_urls.get("Documentation"),
            "Homepage": project_urls.get("Homepage"),
            "versions_detailed": [] 
        }

        # Dependencies queueing
        requires_dist = info.get("requires_dist", [])
        if requires_dist:
            for req in requires_dist:
                dep_name = get_clean_package_name(req)
                if dep_name and dep_name.lower() not in visited_set:
                    package_queue.append(dep_name)

        # Versions Loop
        releases = data.get("releases", {})
        tqdm.write(f"\n--- Processing: {package_name} ({len(releases)} versions) ---")

        for version in tqdm(releases, desc=f"Downloading {package_name}", unit="ver"):
            ver_url = f"https://pypi.org/pypi/{package_name}/{version}/json"
            
            try:
                ver_resp = requests.get(ver_url)
                if ver_resp.status_code != 200:
                    continue
                
                ver_data = ver_resp.json()
                ver_info = ver_data.get("info", {})
                urls_list = ver_data.get("urls", [])
                upload_time = urls_list[0].get("upload_time") if urls_list else None
                
                # Vulnerabilities
                raw_vulns = ver_data.get("vulnerabilities", [])
                processed_vulns = []
                
                for v in raw_vulns:
                    aliases = v.get("aliases", [])
                    cve_alias = None
                    
                    if isinstance(aliases, list) and len(aliases) > 0:
                        cve_alias = aliases[0]
                        if cve_alias and cve_alias.startswith("CVE-"):
                            fetch_nvd_cve_details(cve_alias, cve_database)

                    processed_vulns.append({
                        "cve_id": cve_alias, 
                        "details": v.get("details"),
                        "fixed_in": v.get("fixed_in"),
                        "link": v.get("link")
                    })

                package_entry["versions_detailed"].append({
                    "version": version,
                    "upload_time": upload_time,
                    "requires_dist": ver_info.get("requires_dist"), # Importante per il grafo
                    "requires_python": ver_info.get("requires_python"),
                    "vulnerabilities": processed_vulns
                })
                
            except Exception as e:
                tqdm.write(f"Error on version {version}: {e}")

        return package_entry

    except Exception as e:
        tqdm.write(f"Critical error on {package_name}: {e}")
        return None

def main():
    queue = list(INITIAL_PACKAGES)
    visited = set()
    all_packages_dict = {}
    cve_database_dict = {} 

    print(f"Starting crawl from: {INITIAL_PACKAGES}")
    
    while queue:
        current_package = queue.pop(0)
        if current_package.lower() in visited:
            continue

        package_data = fetch_package_data(current_package, visited, queue, cve_database_dict)
        
        if package_data:
            all_packages_dict[package_data["name"]] = package_data
            save_checkpoint(all_packages_dict, cve_database_dict)
            
        tqdm.write(f"> Queue size: {len(queue)} packages waiting...")

    print("\nAll operations completed successfully.")

if __name__ == "__main__":
    main()