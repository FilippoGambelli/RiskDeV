package it.unipi.riskDeV.DTO;

public record PackageRiskMetricsDTO(
    Double riskScore,
    Integer vulnerabilitiesCount
) {
    // Factory method for not found packages
    public static PackageRiskMetricsDTO zero() {
        return new PackageRiskMetricsDTO(0.0, 0);
    }
}