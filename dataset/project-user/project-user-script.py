import subprocess
import tempfile
import shutil
import sys
import json
import random
import hashlib
from pathlib import Path
from datetime import datetime, timedelta
from typing import Dict, Any, List

# --- Configuration ---
OUTPUT_PROJECTS = "projects.json"
OUTPUT_USERS = "users.json"

PROJECT_COUNT = 50 
USER_COUNT = 100    
ADMIN_COUNT = 5     

PASSWORD_SALT = "static_salt_for_demo_purposes" 

# Core packages to seed projects with specific versions
PACKAGE_VERSIONS = {
    "requests": ["2.31.0"],
    "flask": ["2.2.5"],
    "numpy": ["1.26.2"],
    "pandas": ["2.1.4"],
    "django": ["4.2.7"]
}

# --- Data Generation Constants ---
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


def hash_password(plain_password: str) -> str:
    """Returns SHA256 hash of the password using a static salt."""
    salted = f"{PASSWORD_SALT}{plain_password}"
    return hashlib.sha256(salted.encode("utf-8")).hexdigest()

def random_iso_datetime(start: datetime, end: datetime) -> str:
    """Generates a random ISO 8601 formatted datetime string within a range."""
    seconds_range = int((end - start).total_seconds())
    offset = random.randint(0, seconds_range)
    return (start + timedelta(seconds=offset)).isoformat(timespec="seconds")

def get_pip_list(packages: List[Dict[str, str]]) -> List[Dict[str, str]]:
    """
    Creates a temporary virtual environment, installs specified packages,
    and captures the full dependency tree via 'pip list --format=json'.
    """
    temp_dir = Path(tempfile.mkdtemp())
    venv_dir = temp_dir / "venv"

    try:
        # Create a fresh virtual environment
        subprocess.run([sys.executable, "-m", "venv", str(venv_dir)], check=True)

        # Locate pip executable based on OS
        pip_path = venv_dir / ("Scripts/pip.exe" if sys.platform == "win32" else "bin/pip")

        # Install requested packages
        for pkg in packages:
            subprocess.run(
                [str(pip_path), "install", "--disable-pip-version-check", f"{pkg['name']}=={pkg['version']}"],
                check=True,
                stdout=subprocess.DEVNULL,
                stderr=subprocess.DEVNULL
            )

        # Export installed packages as JSON
        result = subprocess.run(
            [str(pip_path), "list", "--format=json"],
            check=True,
            capture_output=True,
            text=True
        )
        return json.loads(result.stdout)

    except Exception as e:
        print(f"Error resolving dependencies: {e}")
        return []

    finally:
        # Clean up temporary files
        shutil.rmtree(temp_dir, ignore_errors=True)


def generate_projects(count: int) -> List[Dict[str, Any]]:
    """Generates synthetic project data including realistic dependency trees."""
    projects = []
    existing_names = set()

    print(f"--- Generating {count} projects ---")

    for i in range(count):
        # Generate unique project name
        base_name = f"{random.choice(PROJECT_NAME_PREFIXES)}-{random.choice(PROJECT_NAME_SUFFIXES)}"
        name = base_name
        suffix_num = 1
        while name in existing_names:
            suffix_num += 1
            name = f"{base_name}-{suffix_num}"
        existing_names.add(name)

        # Randomly select 1-3 core libraries to install
        initial_packages = random.sample(list(PACKAGE_VERSIONS.keys()), k=random.randint(1, 3))
        requested_packages = [{"name": pkg, "version": random.choice(PACKAGE_VERSIONS[pkg])} for pkg in initial_packages]

        # Resolve full dependency tree via pip
        pip_packages = get_pip_list(requested_packages)

        description = f"{name} is a Python project using {len(initial_packages)} core libraries."
        last_update = random_iso_datetime(datetime(2020, 1, 1), datetime.now())

        projects.append({
            "name": name,
            "description": description,
            "last_update": last_update,
            "python_version": "3.11.14",
            "packages": pip_packages,
            "admin": {},         # To be assigned later
            "collaborators": []  # To be assigned later
        })

        print(f"Project generated: {name} ({i+1}/{count})")

    return projects


def generate_users(count: int) -> List[Dict[str, Any]]:
    """Generates synthetic user profiles with hashed passwords."""
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
            "username": username,
            "first_name": first,
            "last_name": last,
            "email": f"{username}@{random.choice(EMAIL_DOMAINS)}",
            "password": hash_password("Password123"),
            "role": "ROLE_USER",
            "project_names": []
        })

    # Promote random users to admins
    if users:
        admin_indices = random.sample(range(len(users)), k=min(ADMIN_COUNT, len(users)))
        for idx in admin_indices:
            users[idx]["role"] = "ROLE_ADMIN"

    return users


def link_users_and_projects(users: List[Dict[str, Any]], projects: List[Dict[str, Any]]):
    """
    Assigns project admins and random collaborators.
    Ensures data consistency between user profiles and project metadata.
    """
    print("\n--- Linking Users and Projects ---")
    if not users or not projects:
        return

    # 1. Assign a mandatory admin for each project
    for proj in projects:
        admin_user = random.choice(users)
        user_info = {
            "username": admin_user['username'],
            "email": admin_user['email']
        }
        
        proj['admin'] = user_info
        
        # Admin is also a collaborator
        proj['collaborators'].append(user_info)

        if proj['name'] not in admin_user['project_names']:
            admin_user['project_names'].append(proj['name'])

    # 2. Assign random additional projects to users
    for user in users:
        # Each user joins 0 to 3 random projects
        num_projects = random.randint(0, min(3, len(projects)))
        if num_projects == 0:
            continue
            
        assigned_projects = random.sample(projects, k=num_projects)
        
        for proj in assigned_projects:
            # Update user's project list
            if proj['name'] not in user['project_names']:
                user['project_names'].append(proj['name'])
            
            # Update project's collaborator list
            collaborator_entry = {"username": user['username'], "email": user['email']}
            
            if collaborator_entry not in proj['collaborators']:
                proj['collaborators'].append(collaborator_entry)


def main():
    random.seed(42)  # Ensure reproducible results

    projects = generate_projects(PROJECT_COUNT)
    users = generate_users(USER_COUNT)

    link_users_and_projects(users, projects)

    with open(OUTPUT_PROJECTS, "w", encoding="utf-8") as f:
        json.dump(projects, f, indent=2, ensure_ascii=False)
    print(f"Saved {OUTPUT_PROJECTS}")

    with open(OUTPUT_USERS, "w", encoding="utf-8") as f:
        json.dump(users, f, indent=2, ensure_ascii=False)
    print(f"Saved {OUTPUT_USERS}")

    print("\nDONE! Data generation completed.")

if __name__ == "__main__":
    main()