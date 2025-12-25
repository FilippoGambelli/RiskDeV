package it.unipi.riskDeV.DTO;

import lombok.Data;
import it.unipi.riskDeV.model.PackageVersion;
import it.unipi.riskDeV.model.PackageVersion.EmbeddedVulnerability;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
public class PackageVersionDTO {
    @Schema(description = "Name of the package")
    private String packageName;

    @Schema(description = "Version of the package")
    private String version;

    @Schema(description = "Upload time of the package")
    private String uploadTime;

    @Schema(description = "Number of vulnerabilities of the package")
    private int vulnerabilityCount;

    private List<EmbeddedVulnerability> vulnerabilities;

    /**
     * Constructor to convert a PackageVersion model into a PackageVersionDTO
     * @param model the PackageVersion entity from the database
     */
    public PackageVersionDTO(PackageVersion model) {
        this.packageName = model.getPackageName();
        this.version = model.getVersion();
        this.uploadTime = model.getUploadTime();
        this.vulnerabilities = model.getVulnerabilities();
        
        this.vulnerabilityCount = (model.getVulnerabilities() != null) ? model.getVulnerabilities().size() : 0;
    }
}