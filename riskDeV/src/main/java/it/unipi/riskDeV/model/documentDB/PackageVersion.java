package it.unipi.riskDeV.model.documentDB;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "package")
public class PackageVersion {

    @Id
    private String id;

    @Field("package_name")
    private String packageName;

    private String version;

    @Field("version_array")
    private List<Integer> versionArray;

    private String author;

    @Field("author_email")
    private String authorEmail;

    private String description;

    @Field("package_url")
    private String packageURL;

    @Field("documentation")
    private String documentationURL;

    @Field("upload_time")
    private String uploadTime;

    @Field("requires_dist")
    private List<Constraints> dependencies;

    @Field("requires_python")
    private String requiresPython;
    
    @Field("risk_score")
    private Double riskScore;

    private List<EmbeddedVulnerability> vulnerabilities;
}