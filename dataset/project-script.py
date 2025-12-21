import json
import random
import uuid
from datetime import datetime, timedelta

# Allowed Python packages with REAL existing versions
PACKAGE_VERSIONS = {
    "requests": ["2.25.1", "2.26.0", "2.28.2", "2.31.0"],
    "flask": ["1.1.4", "2.0.3", "2.1.3", "2.2.5", "3.0.0"],
    "django": ["2.2.28", "3.2.25", "4.0.10", "4.1.13", "4.2.7"],
    "numpy": ["1.19.5", "1.21.6", "1.23.5", "1.24.4", "1.26.2"],
    "pandas": ["1.2.5", "1.3.5", "1.5.3", "2.0.3", "2.1.4"],
    "pytest": ["6.2.5", "7.1.3", "7.4.4"],
    "python-dotenv": ["0.19.2", "0.21.1", "1.0.0"],
    "pillow": ["8.4.0", "9.5.0", "10.0.1", "10.1.0"],
    "tqdm": ["4.62.3", "4.64.1", "4.66.1"],
    "rich": ["10.16.2", "12.6.0", "13.7.0"],
    "matplotlib": ["3.3.4", "3.5.3", "3.7.4", "3.8.2"]
}

INITIAL_PACKAGES = list(PACKAGE_VERSIONS.keys())

NAME_PREFIXES = [
    "Alpha", "Beta", "Gamma", "Delta", "Echo",
    "Atlas", "Nimbus", "Vector", "Orbit", "Zenith",
    "Vertex", "Aperture", "Pulse", "Lighthouse"
]

NAME_SUFFIXES = [
    "Pipeline", "Service", "Lab", "Engine", "Toolkit",
    "Dashboard", "Collector", "Studio", "Analyzer",
    "Core", "Manager", "Gateway", "Runner"
]

PYTHON_VERSIONS = ["3.7", "3.8", "3.9", "3.10", "3.11", "3.12"]


def random_datetime(start, end):
    delta_seconds = int((end - start).total_seconds())
    random_seconds = random.randint(0, delta_seconds)
    return (start + timedelta(seconds=random_seconds)).isoformat(timespec="seconds")


def generate_project(min_packages=2, max_packages=6):
    project_id = str(uuid.uuid4())

    name = f"{random.choice(NAME_PREFIXES)} {random.choice(NAME_SUFFIXES)}"
    if random.random() < 0.3:
        name += f" {random.randint(1, 99)}"

    selected_packages = random.sample(
        INITIAL_PACKAGES,
        k=random.randint(min_packages, max_packages)
    )

    packages = []
    for pkg in selected_packages:
        packages.append({
            "name": pkg,
            "version": random.choice(PACKAGE_VERSIONS[pkg])
        })

    description = (
        f"{name} is a Python project that uses "
        f"{len(packages)} well-known libraries to support development tasks."
    )

    last_update = random_datetime(
        datetime(2019, 1, 1),
        datetime.now()
    )

    python_version = random.choices(
        PYTHON_VERSIONS,
        weights=[1, 1, 2, 3, 4, 3],
        k=1
    )[0]

    return {
        "_id": project_id,
        "name": name,
        "description": description,
        "last_update": last_update,
        "python_version": python_version,
        "packages": packages
    }

if __name__ == "__main__":
    random.seed(42)

    output_file="projects.json"
    project_count=50

    projects = [generate_project() for _ in range(project_count)]

    with open(output_file, "w", encoding="utf-8") as f:
        json.dump(projects, f, indent=2, ensure_ascii=False)

    print("DONE!")