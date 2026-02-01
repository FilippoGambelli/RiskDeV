package it.unipi.riskDeV.DTO.project;

import io.swagger.v3.oas.annotations.media.Schema;
import it.unipi.riskDeV.model.documentDB.Project.ProjectPackage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
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
    private String version;

    public InstalledPackageDTO(ProjectPackage projectPackage) {
        this.name = projectPackage.getName();
        this.version = projectPackage.getVersion();
    }
}
