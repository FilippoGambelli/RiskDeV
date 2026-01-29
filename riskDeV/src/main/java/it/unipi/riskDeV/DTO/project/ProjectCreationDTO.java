package it.unipi.riskDeV.DTO.project;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@Schema(description = "DTO for creating a new project configuration")
public class ProjectCreationDTO {

    @Schema(description = "Name of the project", example = "RiskAnalysis_AI", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Project name is required")
    @Size(max = 100)
    String name;

    @Schema(description = "Detailed description", example = "Credit risk calculation module", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Project description is required")
    @Size(max = 500)
    String description;

    @Schema(description = "Target Python version", example = "3.11", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Python version is required")
    String pythonVersion;

    @ArraySchema(schema = @Schema(description = "List of dependencies to install"))
    @NotNull(message = "Package list cannot be null")
    @Valid 
    List<InstalledPackageDTO> packages; 
}