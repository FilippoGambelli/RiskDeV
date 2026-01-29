package it.unipi.riskDeV.DTO.packageVersion;

import io.swagger.v3.oas.annotations.media.Schema;
import it.unipi.riskDeV.model.documentDB.Constraints;
import it.unipi.riskDeV.model.documentDB.EmbeddedVulnerability;
import it.unipi.riskDeV.model.documentDB.PackageVersion;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePackageVersionDTO {

    @Schema(description = "Python version requirements", example = ">=3.6")
    private Optional<String> requiresPython;

    @Schema(description = "List of package dependencies (raw strings)", example = "[\"pandas >= 1.0\", \"scipy\"]")
    private List<String> dependencies;

    @Schema(description = "List of known vulnerabilities associated with this version")
    @Valid 
    private List<EmbeddedVulnerabilityDTO> vulnerabilities;

    public UpdatePackageVersionDTO(PackageVersion model) {
        this.vulnerabilities = new ArrayList<>();
        for(EmbeddedVulnerability vulnerabilty: model.getVulnerabilities()) {
            this.vulnerabilities.add(new EmbeddedVulnerabilityDTO(vulnerabilty));
        }
        this.requiresPython = Optional.ofNullable(model.getRequiresPython());
        this.dependencies = new ArrayList<>();
        for(Constraints dependency: model.getDependencies()) {
            this.dependencies.add(dependency.getFull());
        }
    }
}