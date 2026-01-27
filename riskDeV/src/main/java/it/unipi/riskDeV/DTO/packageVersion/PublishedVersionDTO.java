package it.unipi.riskDeV.DTO.packageVersion;

import io.swagger.v3.oas.annotations.media.Schema;
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
    private List<ConstraintsDTO> dependencies;

    @Schema(description = "Calculated risk score based on vulnerabilities", example = "7.5", accessMode = Schema.AccessMode.READ_ONLY)
    private Double riskScore;

    @Schema(description = "List of known vulnerabilities associated with this version")
    @Valid 
    private List<EmbeddedVulnerabilityDTO> vulnerabilities;

}