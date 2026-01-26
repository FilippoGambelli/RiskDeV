package it.unipi.riskDeV.DTO.admin;

public record SeverityDistributionDTO(
    String severity,
    long count,
    double percentage
) {}