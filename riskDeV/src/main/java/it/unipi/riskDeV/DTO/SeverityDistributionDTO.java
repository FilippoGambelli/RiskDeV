package it.unipi.riskDeV.DTO;

public record SeverityDistributionDTO(
    String severity,
    long count,
    double percentage
) {}