package it.unipi.riskDeV.DTO.project;

import io.swagger.v3.oas.annotations.media.Schema;
import it.unipi.riskDeV.model.documentDB.Project;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class InstalledPackageDTO {
    @Schema(
        description = "Name of the package", 
        example = "numpy",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Package name must not be blank")
    @Size(max = 255, message = "Package name must be less than 255 characters")
    private String name;

    @Schema(
        description = "Version of the package following SemVer if possible", 
        example = "1.21.0",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Package version must not be blank")
    @Size(max = 50, message = "Version string is too long")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Version contains invalid characters")
    private String version;

    public static InstalledPackageDTO fromEntity(Project.ProjectPackage projectPackage) {
        if (projectPackage == null) return null;
        return InstalledPackageDTO.builder()
                .name(projectPackage.getName())
                .version(projectPackage.getVersion())
                .build();
    }
}
