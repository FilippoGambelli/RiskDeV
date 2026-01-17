import subprocess
import tempfile
import shutil
import sys
from pathlib import Path
import json
import random
import uuid
from datetime import datetime, timedelta
from typing import Dict, Any
from tqdm import tqdm

# Known Python packages and valid versions
PACKAGE_VERSIONS = {
    "requests": ["2.31.0"],
    "flask": ["2.2.5"],
    "numpy": ["1.26.2"],
    "pandas": ["2.1.4"],
    "django": ["4.2.7"]
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

def get_pip_list(packages: list[dict[str, str]]) -> list[dict[str, str]]:
    """Create a temp venv, install packages, return pip list output."""
    temp_dir = Path(tempfile.mkdtemp())
    venv_dir = temp_dir / "venv"

    try:
        # Create venv
        subprocess.run(
            [sys.executable, "-m", "venv", str(venv_dir)],
            check=True,
        )

        pip = venv_dir / ("Scripts/pip.exe" if sys.platform == "win32" else "bin/pip")

        # Install initial packages
        for pkg in packages:
            subprocess.run(
                [str(pip), "install", "--disable-pip-version-check", f"{pkg['name']}=={pkg['version']}"],
                check=True,
                stdout=subprocess.DEVNULL,
            )

        # Get pip list
        result = subprocess.run(
            [str(pip), "list", "--format=json"],
            check=True,
            capture_output=True,
            text=True,
        )

        return json.loads(result.stdout)

    finally:
        try:
            shutil.rmtree(temp_dir)
        except Exception as e:
            print(f"Warning: could not remove temp dir {temp_dir}: {e}")


def random_iso_datetime(start: datetime, end: datetime) -> str:
    """Return a random ISO 8601 datetime string between start and end."""
    seconds_range = int((end - start).total_seconds())
    offset = random.randint(0, seconds_range)
    return (start + timedelta(seconds=offset)).isoformat(timespec="seconds")


def generate_project(existing_names: set) -> Dict[str, Any]:
    """Generate a random project description with a unique name."""
    base_name = f"{random.choice(PROJECT_NAME_PREFIXES)} {random.choice(PROJECT_NAME_SUFFIXES)}"
    name = base_name

    # Ensure uniqueness
    suffix_num = 1
    while name in existing_names:
        suffix_num += 1
        name = f"{base_name} {suffix_num}"

    existing_names.add(name)

    initial_packages = random.sample(
        list(PACKAGE_VERSIONS.keys()),
        k=random.randint(1, 3),
    )

    requested_packages = [
        {
            "name": pkg,
            "version": random.choice(PACKAGE_VERSIONS[pkg]),
        }
        for pkg in initial_packages
    ]

    pip_packages = get_pip_list(requested_packages)

    description = (
        f"{name} is a Python project that uses {len(initial_packages)} popular libraries "
        f"to support development tasks."
    )

    last_update = random_iso_datetime(
        datetime(2019, 1, 1),
        datetime.now(),
    )

    return {
        "uuid": str(uuid.uuid4()),
        "name": name,
        "description": description,
        "last_update": last_update,
        "python_version": "3.11.14",
        "packages": pip_packages,
    }


def main() -> None:
    random.seed(42)

    output_file = "project.json"
    project_count = 50

    existing_names = set()
    projects = []

    for _ in tqdm(range(project_count), desc="Generating projects"):
        projects.append(generate_project(existing_names))

    with open(output_file, "w", encoding="utf-8") as f:
        json.dump(projects, f, indent=2, ensure_ascii=False)

    print("DONE!")

if __name__ == "__main__":
    main()
