package it.unipi.riskDeV.DTO.project;

import io.swagger.v3.oas.annotations.media.Schema;
import it.unipi.riskDeV.DTO.CollaboratorDTO;
import it.unipi.riskDeV.DTO.InstalledPackageDTO;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.List;

public record ProjectDTO (
    @Schema(description = "Name of the project", example = "Data Analysis Project")
    @NotBlank(message = "Project name is required")
    String name,

    @Schema(description = "Description of the project", example = "A project for data analysis")
    @NotBlank(message = "Project description is required")
    String description,

    @Schema(description = "Date of the last update", example = "2026-05-15T10:15_30Z")
    Instant lastUpdate,

    @Schema(description = "Version of Python used in the project", example = "3.8")
    @NotBlank(message = "Python version is required")
    String pythonVersion,

    @Schema(description = "Administrator of the project")
    CollaboratorDTO admin,

    @Schema(description = "List of packages associated with the project")
    List<InstalledPackageDTO> packages,

    @Schema(description = "List of collaborators in the project")
    List<CollaboratorDTO> collaborators
) {

}
