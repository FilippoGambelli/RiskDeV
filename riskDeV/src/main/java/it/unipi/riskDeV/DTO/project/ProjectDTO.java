package it.unipi.riskDeV.DTO.project;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import it.unipi.riskDeV.model.documentDB.Project;

@Data
@Builder
@AllArgsConstructor
public class ProjectDTO {

    @Schema(description = "Unique identifier name of the project", example = "RiskAnalysis_AI")
    @NotBlank(message = "Project name is mandatory")
    @Size(max = 100, message = "Project name cannot exceed 100 characters")
    private String name;

    @Schema(description = "Brief description of the project scope", example = "AI model for credit risk assessment")
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @Schema(
        description = "Timestamp of the last modification", 
        example = "2023-10-01T12:00:00Z",
        accessMode = Schema.AccessMode.READ_ONLY 
    )
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Instant lastUpdate;

    @Schema(description = "Python interpreter version required", example = "3.9")
    @NotBlank(message = "Python version is mandatory")
    private String pythonVersion;

    @Schema(description = "Project Administrator details")
    @NotNull(message = "Project admin is mandatory")
    @Valid
    private CollaboratorDTO admin;

    @ArraySchema(schema = @Schema(description = "List of python packages installed"))
    @NotNull(message = "Packages list cannot be null (send empty list instead)")
    @Valid
    private List<InstalledPackageDTO> packages;

    @ArraySchema(schema = @Schema(description = "List of project collaborators"))
    @NotNull(message = "Collaborators list cannot be null (send empty list instead)")
    @Valid
    private List<CollaboratorDTO> collaborators;

    public static ProjectDTO fromEntity(Project project) {
        if (project == null) return null;

        return ProjectDTO.builder()
                .name(project.getName())
                .description(project.getDescription())
                .lastUpdate(project.getLastUpdate())
                .pythonVersion(project.getPythonVersion())
                .admin(CollaboratorDTO.fromEntity(project.getAdmin()))
                .packages(
                    Optional.ofNullable(project.getPackages())
                        .orElse(Collections.emptyList())
                        .stream()
                        .map(InstalledPackageDTO::fromEntity)
                        .toList() 
                )
                .collaborators(
                    Optional.ofNullable(project.getCollaborators())
                        .orElse(Collections.emptyList())
                        .stream()
                        .map(CollaboratorDTO::fromEntity)
                        .toList()
                )
                .build();
    }

}