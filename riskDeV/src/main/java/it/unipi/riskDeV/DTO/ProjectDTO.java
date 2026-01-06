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
    
    @Schema(description = "Unique identifier of the project", accessMode = Schema.AccessMode.READ_ONLY)
    private String id;

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

    @Schema(description = "List of packages associated with the project")
    private List<InstalledPackageDTO> packages;
}
