import json
import random
import hashlib
from pathlib import Path
from typing import List, Dict

PROJECTS_FILE = "../project/projects.json"
OUTPUT_FILE = "users.json"
USER_COUNT = 200

PASSWORD_SALT = "static_salt_for_demo_purposes"

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

# Tracks all generated IDs/usernames to ensure uniqueness
existing_ids: set[str] = set()


def hash_password(plain_password: str) -> str:
    salted = f"{PASSWORD_SALT}{plain_password}"
    return hashlib.sha256(salted.encode("utf-8")).hexdigest()


def load_project_ids(filename: str) -> List[str]:
    path = Path(filename)
    if not path.exists():
        raise FileNotFoundError(f"Project file not found: {filename}")

    with open(path, "r", encoding="utf-8") as f:
        projects = json.load(f)

    return [project["_id"] for project in projects]


def make_unique_username(base: str) -> str:
    username = base
    counter = 1
    while username in existing_ids:
        username = f"{base}{counter}"
        counter += 1
    existing_ids.add(username)
    return username


def generate_email(username: str) -> str:
    domain = random.choice(EMAIL_DOMAINS)
    return f"{username}@{domain}"


def generate_users(project_ids: List[str]) -> List[Dict]:
    users: List[Dict] = []

    # Ensure at least one user per project
    for project_id in project_ids:
        first = random.choice(FIRST_NAMES)
        last = random.choice(LAST_NAMES)

        base_username = f"{first.lower()}.{last.lower()}"
        username = make_unique_username(base_username)

        users.append(
            {
                "_id": username,
                "first_name": first,
                "last_name": last,
                "username": username,
                "email": generate_email(username),
                "password": hash_password("Password123"),
                "project_ids": [project_id],
            }
        )

    # Generate remaining users
    remaining_users = USER_COUNT - len(users)

    for _ in range(remaining_users):
        first = random.choice(FIRST_NAMES)
        last = random.choice(LAST_NAMES)

        base_username = f"{first.lower()}.{last.lower()}"
        username = make_unique_username(base_username)

        # 25% with no projects; otherwise 1–3 random projects
        if random.random() < 0.25:
            project_list: List[str] = []
        else:
            project_list = random.sample(
                project_ids,
                k=random.randint(1, min(3, len(project_ids))),
            )

        users.append(
            {
                "_id": username,
                "first_name": first,
                "last_name": last,
                "username": username,
                "email": generate_email(username),
                "password": hash_password("Password123"),
                "project_ids": project_list,
            }
        )

    return users


def main() -> None:
    random.seed(42)

    project_ids = load_project_ids(PROJECTS_FILE)
    users = generate_users(project_ids)

    with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
        json.dump(users, f, indent=2, ensure_ascii=False)

    print("Done!")


if __name__ == "__main__":
    main()