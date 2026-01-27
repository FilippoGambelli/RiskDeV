package it.unipi.riskDeV.DTO.packageVersion;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Optional;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePackageVersionDTO {

    @Schema(description = "Upload time of the package (Server generated)", accessMode = Schema.AccessMode.READ_ONLY)
    private Optional<String> uploadTime;

    @Schema(description = "Python version requirements", example = ">=3.6")
    @Size(max = 50, message = "Python requirements string is too long")
    private Optional<String> requiresPython;

    @Schema(description = "List of package dependencies (raw strings)", example = "[\"pandas >= 1.0\", \"scipy\"]")
    private List<ConstraintsDTO> dependencies;

    @Schema(description = "List of known vulnerabilities associated with this version")
    @Valid 
    private List<EmbeddedVulnerabilityDTO> vulnerabilities;

}