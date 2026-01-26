package it.unipi.riskDeV.DTO.admin;

public record PackageRiskTrendDTO(

    // The unique name of the package (e.g. "requests", "numpy", "flask").
    String packageName,

    // The total number of versions of this package stored in the system.
    int totalVersions,

    /**
     * The number of security regressions detected across versions.
     * A regression is counted when a newer version:
     *  - Does not reduce the risk score compared to the previous version, OR
     *  - Introduces more vulnerabilities (CVEs) than the previous version.
     *
     * This value indicates how often the package fails to improve its security posture.
     */
    int regressions,

    // The average risk score across all recorded versions of the package.
    double avgRisk,

    // The risk score of the most recent version of the package.
    double latestRisk

) {}