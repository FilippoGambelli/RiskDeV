import json
import random
import uuid
from datetime import datetime, timedelta
from typing import List, Dict, Any

# Known Python packages and valid versions
PACKAGE_VERSIONS = {
    "requests": ["2.25.1", "2.26.0", "2.28.2", "2.31.0"],
    "flask": ["1.1.4", "2.0.3", "2.1.3", "2.2.5", "3.0.0"],
    "django": ["2.2.28", "3.2.25", "4.0.10", "4.1.13", "4.2.7"],
    "numpy": ["1.19.5", "1.21.6", "1.23.5", "1.24.4", "1.26.2"],
    "pandas": ["1.2.5", "1.3.5", "1.5.3", "2.0.3", "2.1.4"],
    "tqdm": ["4.62.3", "4.64.1", "4.66.1"],
    "matplotlib": ["3.3.4", "3.5.3", "3.7.4", "3.8.2"],
}

PACKAGE_NAMES = list(PACKAGE_VERSIONS.keys())

PROJECT_NAME_PREFIXES = [
    "Alpha", "Beta", "Gamma", "Delta", "Echo",
    "Atlas", "Nimbus", "Vector", "Orbit", "Zenith",
    "Vertex", "Aperture", "Pulse", "Lighthouse",
]

PROJECT_NAME_SUFFIXES = [
    "Pipeline", "Service", "Lab", "Engine", "Toolkit",
    "Dashboard", "Collector", "Studio", "Analyzer",
    "Core", "Manager", "Gateway", "Runner",
]

PYTHON_VERSIONS = ["3.7", "3.8", "3.9", "3.10", "3.11", "3.12"]


def random_iso_datetime(start: datetime, end: datetime) -> str:
    """Return a random ISO 8601 datetime string between start and end."""
    seconds_range = int((end - start).total_seconds())
    offset = random.randint(0, seconds_range)
    return (start + timedelta(seconds=offset)).isoformat(timespec="seconds")


def generate_project(existing_names: set, min_packages: int = 2, max_packages: int = 6) -> Dict[str, Any]:
    """Generate a random project description with a unique name."""
    base_name = f"{random.choice(PROJECT_NAME_PREFIXES)} {random.choice(PROJECT_NAME_SUFFIXES)}"
    name = base_name

    # Ensure uniqueness
    suffix_num = 1
    while name in existing_names:
        suffix_num += 1
        name = f"{base_name} {suffix_num}"

    existing_names.add(name)

    selected_packages = random.sample(
        PACKAGE_NAMES,
        k=random.randint(min_packages, max_packages),
    )

    packages: List[Dict[str, str]] = [
        {
            "name": pkg,
            "version": random.choice(PACKAGE_VERSIONS[pkg]),
        }
        for pkg in selected_packages
    ]

    description = (
        f"{name} is a Python project that uses {len(packages)} popular libraries "
        f"to support development tasks."
    )

    last_update = random_iso_datetime(
        datetime(2019, 1, 1),
        datetime.now(),
    )

    python_version = random.choices(
        PYTHON_VERSIONS,
        weights=[1, 1, 2, 3, 4, 3],
        k=1,
    )[0]

    return {
        "_id": name,
        "description": description,
        "last_update": last_update,
        "python_version": python_version,
        "packages": packages,
    }


def main() -> None:
    random.seed(42)

    output_file = "projects.json"
    project_count = 50

    existing_names = set()
    projects = [generate_project(existing_names) for _ in range(project_count)]

    with open(output_file, "w", encoding="utf-8") as f:
        json.dump(projects, f, indent=2, ensure_ascii=False)

    print("DONE!")

if __name__ == "__main__":
    main()
