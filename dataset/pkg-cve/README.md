This folder contains **two Python scripts** that crawl Python packages from PyPI, collect metadata and vulnerabilities, normalize package versions into sortable arrays, and calculate risk scores for each version.

---

### Scripts Overview

### 1. `pkg-cve-script.py.py`

**Purpose:**
Fetch detailed metadata and vulnerabilities for Python packages from PyPI and NVD (National Vulnerability Database).

**What it does:**

* Starts from an initial list of packages (`INITIAL_PACKAGES`).
* Fetches package metadata from PyPI:

  * `name`, `author`, `description`
  * Available `versions` and `dependencies`
* For each package version, fetches associated vulnerabilities from PyPI.
* Queries NVD API for **detailed CVE metrics**:

  * Prioritizes CVSS v4.0 > v3.1 > v3.0 > v2.0
  * Flattens CVSS metrics for easy scoring

**Output files:**

* `allInfoPackages.json` — package metadata including versions and dependencies
* `vulnerability.json` — detailed CVE info with flattened CVSS metrics

### 2. `generate-package-collection.py`

**Purpose:**
Processes the raw package metadata and vulnerability data to calculate **risk scores** and normalize package **versions into sortable numeric arrays**.

**What it does:**

* Loads `allInfoPackages.json` (package metadata) and `vulnerability.json` (CVE data)

* For each package version:

  * **Calculates risk score:** Maximum CVSS score among associated CVEs

  * **Normalizes versions:** Converts version strings into **fixed-length numeric arrays** for sorting and comparison

    **Examples (MAX_VERSION_LEN = 6):**

    | Version  | Version Array         | Notes                         |
    | -------- | --------------------- | ----------------------------- |
    | `2.0`    | `[2, 0, 0, 0, 0, 0]`  | Simple release                |
    | `2.0.1`  | `[2, 0, 1, 0, 0, 0]`  | Patch release                 |
    | `4.1a1`  | `[4, 1, 0, -3, 1, 0]` | Alpha pre-release (`a` → -3)  |
    | `1.0rc2` | `[1, 0, 0, -1, 2, 0]` | Release candidate (`rc` → -1) |

  * **Parses dependencies (`requires_dist`)** into structured format:

    ```json
    {
        "name": "certifi",
        "version_gte": "2017.4.17",
        "version_lte": null,
        "version_gt": null,
        "version_lt": null,
        "version_eq": null,
        "version_neq": null
    }
    ```

* Builds final structured entry for each package version:

  * `package_name`, `version`, `version_array`
  * Metadata: `author`, `description`, `documentation`
  * Structured dependencies and Python requirements
  * Vulnerabilities and `risk_score`

**Output file:**

* `package.json` — fully processed package data, ready for analysis or sorting:

  * `version_array` for comparison
  * `risk_score` for vulnerabilities
  * Metadata and structured dependencies

---

### Workflow

1. **Fetch package info and CVEs**

```bash
python pkg-cve-script.py
```

* Crawls PyPI and NVD, saving:

  * `allInfoPackages.json`
  * `vulnerability.json`

2. **Process versions, normalize, and calculate risk scores**

```bash
python generate-package-collection.py
```

* Reads the raw JSON files
* Normalizes versions into arrays
* Calculates risk scores
* Parses dependencies into structured format
* Saves `package.json`

---

### Example Entry in `package.json`

```json
{
    "package_name": "requests",
    "version": "2.31.0",
    "version_array": [2, 31, 0, 0, 0, 0],
    "author": "Kenneth Reitz",
    "author_email": "me@kennethreitz.org",
    "description": "Python HTTP for Humans.",
    "package_url": "https://pypi.org/project/requests/",
    "documentation": "https://requests.readthedocs.io/",
    "upload_time": "2023-01-15T12:00:00",
    "requires_dist": [
        {
            "name": "certifi",
            "version_gte": "2017.4.17",
            "version_lte": null,
            "version_gt": null,
            "version_lt": null,
            "version_eq": null,
            "version_neq": null
        },
        {
            "name": "charset-normalizer",
            "version_gte": "2.0.0",
            "version_lte": null,
            "version_gt": null,
            "version_lt": null,
            "version_eq": null,
            "version_neq": null
        }
    ],
    "requires_python": ">=3.7",
    "vulnerabilities": [
        {
            "cve_id": "CVE-2022-12345",
            "details": "...",
            "fixed_in": "2.31.1",
            "link": "..."
        }
    ],
    "risk_score": 7.5
}
```