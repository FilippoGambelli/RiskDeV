package it.unipi.riskDeV.DTO.packageVersion;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;
import it.unipi.riskDeV.model.documentDB.Constraints;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(name = "ConstraintsDTO", description = "DTO representing version constraints for a package")
public class ConstraintsDTO {

    @Schema(
        description = "Full version constraint expression",
        example = "urllib3 (<1.27,>=1.21.1)"
    )
    private String full;

    @Schema(
        description = "Name of the package or dependency",
        example = "urllib3"
    )
    private String name;

    @Schema(
        description = "Minimum allowed version (inclusive)",
        example = "1.21.1"
    )
    private String versionGte;

    @Schema(
        description = "Maximum allowed version (inclusive)",
        example = "2.0.0"
    )
    private String versionLte;

    @Schema(
        description = "Version must be greater than this value (exclusive)",
        example = "1.1.5"
    )
    private String versionGt;

    @Schema(
        description = "Version must be less than this value (exclusive)",
        example = "1.27.0"
    )
    private String versionLt;

    @Schema(
        description = "Version must be equal to this value",
        example = "1.3.4"
    )
    private String versionEq;

    @Schema(
        description = "Version must not be equal to this value",
        example = "1.2.5"
    )
    private String versionNeq;

    public ConstraintsDTO(Constraints constraint) {
        this.full = constraint.getFull();
        this.name = constraint.getName();
        this.versionGte = constraint.getVersionGte();
        this.versionLte = constraint.getVersionLte();
        this.versionGt = constraint.getVersionGt();
        this.versionLt = constraint.getVersionLt();
        this.versionEq = constraint.getVersionEq();
        this.versionNeq = constraint.getVersionNeq();
    }
}