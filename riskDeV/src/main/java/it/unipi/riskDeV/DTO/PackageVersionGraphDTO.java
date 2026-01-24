package it.unipi.riskDeV.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import it.unipi.riskDeV.model.neo4j.PackageVersionNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PackageVersionGraphDTO {

    @Schema(description = "package name")
    private String packageName;

    @Schema(description = "package version")
    private String version;

    @Schema(description = "risk score")
    private Double riskScore;

    @Schema(description = "requires python")
    private String requiresPython;

    @Schema(description = "documentation")
    private String documentation;

    public PackageVersionGraphDTO(PackageVersionNode packageVersionNode) {
        this.packageName = packageVersionNode.getPackageName();
        this.version = packageVersionNode.getVersion();
        this.riskScore = packageVersionNode.getRiskScore();
        this.requiresPython = packageVersionNode.getRequiresPython();
        this.documentation = packageVersionNode.getDocumentation();

    }
}
