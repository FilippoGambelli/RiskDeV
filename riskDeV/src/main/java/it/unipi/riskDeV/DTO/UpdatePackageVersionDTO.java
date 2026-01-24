package it.unipi.riskDeV.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import it.unipi.riskDeV.model.PackageVersion;
import it.unipi.riskDeV.model.PackageVersion.Constraints;
import it.unipi.riskDeV.model.PackageVersion.EmbeddedVulnerability;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePackageVersionDTO {

    @Schema(description = "Upload time of the package (Server generated)", accessMode = Schema.AccessMode.READ_ONLY)
    private String uploadTime;

    @Schema(description = "Python version requirements", example = ">=3.6")
    @Size(max = 50, message = "Python requirements string is too long")
    private String requiresPython;

    @Schema(description = "List of package dependencies (raw strings)", example = "[\"pandas >= 1.0\", \"scipy\"]")
    private List<Constraints> dependencies;

    @Schema(description = "List of known vulnerabilities associated with this version")
    @Valid 
    private List<EmbeddedVulnerability> vulnerabilities;

    public UpdatePackageVersionDTO(PackageVersion model) {
        this.uploadTime = model.getUploadTime();
        this.vulnerabilities = model.getVulnerabilities();
        this.requiresPython = model.getRequiresPython();
        this.dependencies = model.getDependencies(); 
    }
}