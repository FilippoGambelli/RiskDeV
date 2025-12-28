import json

def main():
    with open("packages.json", "r", encoding="utf-8") as f:
            data = json.load(f)

    general_packages = []
    package_versions = []

    for pkg in data:
        version_refs = []

        # Process versions
        for ver in pkg.get("versions_detailed", []):

            # Reference saved in general_packages
            version_refs.append(ver["version"])

            # Detailed version document
            package_versions.append({
                "_id": pkg["_id"] + ' ' + ver["version"],
                "package_name": pkg["_id"],
                "version": ver["version"],
                "upload_time": ver.get("upload_time"),
                "requires_dist": ver.get("requires_dist"),
                "requires_python": ver.get("requires_python"),
                "vulnerabilities": ver.get("vulnerabilities", [])
            })

        # General package document
        general_packages.append({
            "_id": pkg["_id"],
            "author": pkg.get("author"),
            "author_email": pkg.get("author_email"),
            "description": pkg.get("description"),
            "package_url": pkg.get("package_url"),
            "summary": pkg.get("summary"),
            "documentation": pkg.get("Documentation"),
            "homepage": pkg.get("Homepage"),
            "versions": version_refs
        })

    # Save outputs
    with open("generalPackages.json", "w", encoding="utf-8") as f:
        json.dump(general_packages, f, indent=4)

    with open("packageVersions.json", "w", encoding="utf-8") as f:
        json.dump(package_versions, f, indent=4)

    print("Done: 'generalPackages.json' and 'packageVersions.json' created.")

if __name__ == "__main__":
    main()