This folder contains **two scripts** that crawl Python packages from PyPI, collect metadata and vulnerabilities, and normalize package versions into sortable arrays for analysis.

---

### Scripts Overview

### 1. `fetch_package_info.py`

**Purpose:**
Fetches detailed metadata and vulnerabilities for Python packages from PyPI and NVD (National Vulnerability Database).

**What it does:**

* Starts from an initial list of packages (`INITIAL_PACKAGES`).
* Fetches package metadata from PyPI (`name`, `author`, `description`, `versions`, `dependencies`).
* For each package version, fetches associated vulnerabilities from PyPI.
* For CVEs, queries NVD API to get **detailed CVSS metrics**.
* Normalizes metrics:

  * Prioritizes CVSS v4.0 > v3.1 > v3.0 > v2.0.
  * Flattens CVSS metrics for easy scoring.


**Output files:**

* `allInfoPackages.json` — package metadata including versions and dependencies.
* `vulnerability.json` — detailed CVE info with flattened CVSS metrics.


### 2. `process_versions_and_scores.py`

**Purpose:**
Processes the raw package metadata and vulnerability data to calculate **risk scores** and normalize package **versions into vectors** for sorting and comparison.

**What it does:**

* Loads `allInfoPackages.json` (package metadata) and `vulnerability.json` (CVE data).
* For each package version:

  * Calculates **risk score** as the **maximum CVSS score** among associated CVEs.
  * Converts version strings (like `2.0`, `4.1a1`, `1.0rc2`) into **fixed-length numeric arrays** called **version vectors**.

    * Example logic:

      * `2.0` → `[2, 0, 0, 0, 0, 0]`
      * `2.0.1` → `[2, 0, 1, 0, 0, 0]`
      * `4.1a1` → `[4, 1, 0, -3, 1, 0]` (`a` → alpha → -3)
      * `1.0rc2` → `[1, 0, 0, -1, 2, 0]` (`rc` → release candidate → -1)
    * Handles pre-release (`alpha`, `beta`, `rc`), post-release (`pl`, `post`), and dev releases with numeric weights.
  * Builds final structured entry for each package version:

    * `package_name`, `version`, `version_array`
    * Metadata (`author`, `description`, `documentation`)
    * Dependencies and Python requirements
    * Vulnerabilities and risk score

**Output file:**

* `package.json` — fully processed package data with:

  * `version_array` for sorting
  * `risk_score` for vulnerabilities
  * Metadata and dependencies

---

### Workflow

1. **Fetch package info and CVEs first**

   ```bash
   python fetch_package_info.py
   ```

   * This will crawl PyPI and NVD, saving `allInfoPackages.json` and `vulnerability.json`.

2. **Process versions and calculate risk scores**

   ```bash
   python process_versions_and_scores.py
   ```

   * This will read the raw JSON files, normalize versions, calculate risk scores, and save `package.json`.

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
    "requires_dist": ["certifi>=2017.4.17", "charset-normalizer>=2.0.0"],
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