package it.unipi.riskDeV.DTO.project;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProjectCreationDTO(
    @NotBlank(message = "Project name is required")
    String name,

    @NotBlank(message = "Project description is required")
    String description,

    @NotBlank(message = "Python version is required")
    String pythonVersion,

    @Valid
    @NotNull(message = "Package list cannot be null (send empty list instead)")
    List<PackageInput> packages
) {
    public record PackageInput(
        @NotBlank(message = "Package name is required")
        String name,
        
        @NotBlank(message = "Package version is required")
        String version
    ) {}
}