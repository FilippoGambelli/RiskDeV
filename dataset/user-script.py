import json
import random
import hashlib
import uuid
from pathlib import Path

PROJECTS_FILE = "projects.json"
OUTPUT_FILE = "users.json"
USER_COUNT = 200

PASSWORD_SALT = "static_salt_for_demo_purposes"

# Simple name pools
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


def hash_password(plain_password):
    salted = f"{PASSWORD_SALT}{plain_password}"
    return hashlib.sha256(salted.encode("utf-8")).hexdigest()


def load_project_ids(filename):
    path = Path(filename)
    if not path.exists():
        raise FileNotFoundError(f"Project file not found: {filename}")

    with open(path, "r", encoding="utf-8") as f:
        projects = json.load(f)

    return [project["_id"] for project in projects]


def generate_username(first: str, last: str, index: int) -> str:
    return f"{first.lower()}.{last.lower()}{index}"


def generate_email(username):
    domain = random.choice(EMAIL_DOMAINS)
    return f"{username}@{domain}"

def generate_users(project_ids: list[str]) -> list[dict]:
    users = []

    # Step 1: ensure at least one user per project
    for idx, project_id in enumerate(project_ids):
        first = random.choice(FIRST_NAMES)
        last = random.choice(LAST_NAMES)
        username = generate_username(first, last, idx)

        user = {
            "_id": str(uuid.uuid4()),
            "first_name": first,
            "last_name": last,
            "username": username,
            "email": generate_email(username),
            "password": hash_password("Password123"),
            "project_ids": [project_id]
        }
        users.append(user)

    # Step 2: generate remaining users
    remaining_users = USER_COUNT - len(users)

    for i in range(remaining_users):
        first = random.choice(FIRST_NAMES)
        last = random.choice(LAST_NAMES)
        username = generate_username(first, last, i + 1000)

        # Randomly decide how many projects the user belongs to
        if random.random() < 0.25:
            # 25% of users have no projects
            project_list = []
        else:
            project_list = random.sample(
                project_ids,
                k=random.randint(1, min(3, len(project_ids)))
            )

        user = {
            "_id": str(uuid.uuid4()),
            "first_name": first,
            "last_name": last,
            "username": username,
            "email": generate_email(username),
            "password": hash_password("Password123"),
            "project_ids": project_list
        }
        users.append(user)

    return users

if __name__ == "__main__":
    random.seed(42)

    project_ids = load_project_ids(PROJECTS_FILE)
    users = generate_users(project_ids)

    with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
        json.dump(users, f, indent=2, ensure_ascii=False)

    print("DONE!")