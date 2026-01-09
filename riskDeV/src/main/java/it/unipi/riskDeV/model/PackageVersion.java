package it.unipi.riskDeV.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

@Data
@Document(collection = "package_version")
public class PackageVersion {

    @Id
    private String id;

    @Field("package_name")
    private String packageName;

    private String version;

    @Field("upload_time")
    private String uploadTime;

    @Field("requires_dist")
    private List<String> dependencies;

    @Field("requires_python")
    private String requiresPython;

    private List<EmbeddedVulnerability> vulnerabilities;

    @Data
    public static class EmbeddedVulnerability {

        @Field("cve_id")
        private String cveId;

        private String details;

        @Field("fixed_in")
        private List<String> fixedIn;
        
        private String link;

    }

}