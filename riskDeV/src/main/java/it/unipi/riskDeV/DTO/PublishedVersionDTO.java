package it.unipi.riskDeV.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import it.unipi.riskDeV.model.PackageVersion;
import it.unipi.riskDeV.model.PackageVersion.Constraints;
import it.unipi.riskDeV.model.PackageVersion.EmbeddedVulnerability;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PublishedVersionDTO {

    @Schema(description = "Name of the package", example = "numpy", requiredMode = Schema.RequiredMode.REQUIRED)
    private String packageName;

    @Schema(description = "Version of the package (SemVer)", example = "1.21.0", requiredMode = Schema.RequiredMode.REQUIRED)
    private String version;

    private List<Integer> versionArray;

    @Schema(description = "Description of the package")
    private String description;

    @Schema(description = "The official documentation URL of the package")
    private String documentationURL;

    @Schema(description = "Python version requirements", example = ">=3.6")
    private String requiresPython;

    @Schema(description = "List of package dependencies (raw strings)", example = "[\"pandas >= 1.0\", \"scipy\"]")
    private List<Constraints> dependencies;

    @Schema(description = "Calculated risk score based on vulnerabilities", example = "7.5", accessMode = Schema.AccessMode.READ_ONLY)
    private Double riskScore;

    @Schema(description = "List of known vulnerabilities associated with this version")
    @Valid 
    private List<EmbeddedVulnerability> vulnerabilities;

    public PublishedVersionDTO(PackageVersion model) {
        this.packageName = model.getPackageName();
        this.version = model.getVersion();
        this.versionArray = model.getVersionArray();
        this.vulnerabilities = model.getVulnerabilities();
        this.requiresPython = model.getRequiresPython();
        this.dependencies = model.getDependencies(); 
        this.description = model.getDescription();
        this.documentationURL = model.getDocumentationURL();
        this.riskScore = model.getRiskScore();
    }
}