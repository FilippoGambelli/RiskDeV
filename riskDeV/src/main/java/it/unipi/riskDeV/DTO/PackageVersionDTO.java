package it.unipi.riskDeV.DTO;

import lombok.Data;
import it.unipi.riskDeV.model.PackageVersion;
import java.util.List;

@Data
public class PackageVersionDTO {
    private String packageName;
    private String version;
    private String uploadTime;
    private int vulnerabilityCount;
    private List<PackageVersion.EmbeddedVulnerability> vulnerabilities;

    public PackageVersionDTO(PackageVersion model) {
        this.packageName = model.getPackage_name();
        this.version = model.getVersion();
        this.uploadTime = model.getUpload_time();
        this.vulnerabilities = model.getVulnerabilities();
        
        // Counting number of vulnerabilities
        this.vulnerabilityCount = (model.getVulnerabilities() != null) ? model.getVulnerabilities().size() : 0;
    }
}