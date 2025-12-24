package it.unipi.riskDeV.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

@Data
@Document(collection = "package_versions")
public class PackageVersion {

    @Id
    private String id;
    private String package_id;
    private String package_name;
    private String version;
    private String upload_time;
    private List<EmbeddedVulnerability> vulnerabilities;

    @Data
    public static class EmbeddedVulnerability {

        private String cve_id;
        private String details;
        private List<String> fixed_in;
        private String link;

    }

}