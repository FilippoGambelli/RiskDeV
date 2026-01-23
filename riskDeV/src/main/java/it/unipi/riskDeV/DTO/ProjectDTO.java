package it.unipi.riskDeV.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProjectDTO {
    @Schema(description = "Name of the project", example = "Data Analysis Project")
    @NotBlank(message = "Project name is required")
    private String name;

    @Schema(description = "Description of the project", example = "A project for data analysis")
    @NotBlank(message = "Project description is required")
    private String description;

    @Schema(description = "Date of the last update", accessMode = Schema.AccessMode.READ_ONLY)
    private Instant lastUpdate;

    @Schema(description = "Version of Python used in the project", example = "3.8")
    @NotBlank(message = "Python version is required")
    private String pythonVersion;

    @Schema(description = "Administrator ID of the project", example = "609e129e8a1b2c0015b8f123")
    private String adminId;

    @Schema(description = "List of packages associated with the project")
    private List<InstalledPackageDTO> packages;

    @Schema(description = "List of collaborators in the project")
    private List<CollaboratorDTO> collaborators;
}
