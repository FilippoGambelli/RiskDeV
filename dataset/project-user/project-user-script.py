import subprocess
import tempfile
import shutil
import sys
import json
import random
import uuid
import hashlib
from pathlib import Path
from datetime import datetime, timedelta
from typing import Dict, Any, List

# --- CONFIGURATION ---
OUTPUT_PROJECTS = "projects.json"
OUTPUT_USERS = "users.json"

PROJECT_COUNT = 20  # Keep in mind: generating virtual envs takes time.
USER_COUNT = 50
ADMIN_COUNT = 5

PASSWORD_SALT = "static_salt_for_demo_purposes"

# --- DATA CONSTANTS ---

PACKAGE_VERSIONS = {
    "requests": ["2.31.0"],
    "flask": ["2.2.5"],
    "numpy": ["1.26.2"],
    "pandas": ["2.1.4"],
    "django": ["4.2.7"]
}

PROJECT_NAME_PREFIXES = [
    "Alpha", "Beta", "Gamma", "Delta", "Echo", "Atlas", "Nimbus", 
    "Vector", "Orbit", "Zenith", "Vertex", "Aperture", "Pulse", "Lighthouse"
]

PROJECT_NAME_SUFFIXES = [
    "Pipeline", "Service", "Lab", "Engine", "Toolkit", "Dashboard", 
    "Collector", "Studio", "Analyzer", "Core", "Manager", "Gateway"
]

FIRST_NAMES = [
    "John", "Jane", "Michael", "Emily", "David", "Sarah", "Robert", "Laura",
    "Daniel", "Anna", "Marco", "Luca", "Giulia", "Francesca", "Paolo",
    "Alice", "Tom", "Chris", "Sophia", "Emma"
]

LAST_NAMES = [
    "Smith", "Johnson", "Brown", "Taylor", "Anderson", "Martin", "Rossi",
    "Bianchi", "Romano", "Ferrari", "Esposito", "Russo", "Conti",
    "Miller", "Wilson", "Moore"
]

EMAIL_DOMAINS = ["example.com", "mail.com", "company.io", "test.org"]

# --- UTILITY FUNCTIONS ---

def hash_password(plain_password: str) -> str:
    """Create SHA256 hash with salt."""
    salted = f"{PASSWORD_SALT}{plain_password}"
    return hashlib.sha256(salted.encode("utf-8")).hexdigest()

def random_iso_datetime(start: datetime, end: datetime) -> str:
    """Return a random ISO 8601 datetime string."""
    seconds_range = int((end - start).total_seconds())
    offset = random.randint(0, seconds_range)
    return (start + timedelta(seconds=offset)).isoformat(timespec="seconds")

def get_pip_list(packages: list[dict[str, str]]) -> list[dict[str, str]]:
    """
    Creates a temporary venv, installs packages, and returns 'pip list' output via JSON.
    This ensures realistic vulnerability scanning simulation.
    """
    temp_dir = Path(tempfile.mkdtemp())
    venv_dir = temp_dir / "venv"

    try:
        # Create virtual environment
        subprocess.run([sys.executable, "-m", "venv", str(venv_dir)], check=True)
        
        # Determine pip path based on OS
        pip = venv_dir / ("Scripts/pip.exe" if sys.platform == "win32" else "bin/pip")

        # Install requested packages
        for pkg in packages:
            subprocess.run(
                [str(pip), "install", "--disable-pip-version-check", f"{pkg['name']}=={pkg['version']}"],
                check=True,
                stdout=subprocess.DEVNULL,
                stderr=subprocess.DEVNULL 
            )

        # Get installed packages list in JSON format
        result = subprocess.run(
            [str(pip), "list", "--format=json"],
            check=True,
            capture_output=True,
            text=True,
        )
        return json.loads(result.stdout)
    except Exception as e:
        print(f"Error generating packages: {e}")
        return [] 
    finally:
        shutil.rmtree(temp_dir, ignore_errors=True)

# --- PROJECT GENERATION ---

def generate_projects(count: int) -> List[Dict]:
    projects = []
    existing_names = set()
    
    print(f"--- Generating {count} projects (this may take time due to venv creation) ---")
    
    for i in range(count):
        # Generate unique name with HYPHENS instead of spaces
        base_name = f"{random.choice(PROJECT_NAME_PREFIXES)}-{random.choice(PROJECT_NAME_SUFFIXES)}"
        name = base_name
        suffix_num = 1
        
        # Ensure uniqueness
        while name in existing_names:
            suffix_num += 1
            name = f"{base_name}-{suffix_num}"
        
        existing_names.add(name)

        # Select random packages
        initial_packages = random.sample(list(PACKAGE_VERSIONS.keys()), k=random.randint(1, 3))
        requested_packages = [{"name": pkg, "version": random.choice(PACKAGE_VERSIONS[pkg])} for pkg in initial_packages]
        
        # Get real pip metadata
        pip_packages = get_pip_list(requested_packages)

        description = f"{name} is a Python project using {len(initial_packages)} core libraries."
        last_update = random_iso_datetime(datetime(2020, 1, 1), datetime.now())

        projects.append({
            "_id": str(uuid.uuid4()),
            "name": name,
            "description": description,
            "last_update": last_update,
            "python_version": "3.11.14",
            "packages": pip_packages,
            "collaborators": [] # To be filled later
        })
        
        print(f"Project generated: {name} ({i+1}/{count})")

    return projects

# --- USER GENERATION ---

def generate_users(count: int) -> List[Dict]:
    users = []
    existing_usernames = set()
    
    print(f"\n--- Generating {count} users ---")

    for _ in range(count):
        first = random.choice(FIRST_NAMES)
        last = random.choice(LAST_NAMES)
        
        base_username = f"{first.lower()}.{last.lower()}"
        username = base_username
        counter = 1
        
        # Ensure unique username
        while username in existing_usernames:
            username = f"{base_username}{counter}"
            counter += 1
        existing_usernames.add(username)

        users.append({
            "_id": str(uuid.uuid4()),
            "username": username,
            "first_name": first,
            "last_name": last,
            "email": f"{username}@{random.choice(EMAIL_DOMAINS)}",
            "password": hash_password("Password123"),
            "role": "ROLE_USER",
            "project_names": [] # To be filled later
        })

    # Assign Admin roles randomly
    if users:
        admin_indices = random.sample(range(len(users)), k=min(ADMIN_COUNT, len(users)))
        for idx in admin_indices:
            users[idx]["role"] = "ROLE_ADMIN"

    return users

# --- DATA LINKING LOGIC ---

def link_users_and_projects(users: List[Dict], projects: List[Dict]):
    """
    Assigns projects to users and updates both entities:
    1. Users get a list of project names.
    2. Projects get a list of collaborators (username/email).
    """
    print("\n--- Linking Users and Projects ---")
    
    if not projects:
        return

    for user in users:
        # Assign 0 to 3 random projects to this user
        num_projects = random.randint(0, min(3, len(projects)))
        
        if num_projects > 0:
            assigned_projects = random.sample(projects, k=num_projects)
            
            for proj in assigned_projects:
                # 1. Update User: Add project name
                user['project_names'].append(proj['name'])
                
                # 2. Update Project: Add collaborator info
                collaborator_entry = {
                    "username": user['username'],
                    "email": user['email']
                }
                
                # Avoid duplicates in the project's collaborator list
                if collaborator_entry not in proj['collaborators']:
                    proj['collaborators'].append(collaborator_entry)

# --- MAIN EXECUTION ---

def main():
    random.seed(42)

    # 1. Generate Projects
    projects = generate_projects(PROJECT_COUNT)

    # 2. Generate Users
    users = generate_users(USER_COUNT)

    # 3. Link Entities
    link_users_and_projects(users, projects)

    # 4. Save Projects
    with open(OUTPUT_PROJECTS, "w", encoding="utf-8") as f:
        json.dump(projects, f, indent=2, ensure_ascii=False)
    print(f"Saved {OUTPUT_PROJECTS}")

    # 5. Save Users
    with open(OUTPUT_USERS, "w", encoding="utf-8") as f:
        json.dump(users, f, indent=2, ensure_ascii=False)
    print(f"Saved {OUTPUT_USERS}")

    print("\nDONE! Data generation completed.")

if __name__ == "__main__":
    main()