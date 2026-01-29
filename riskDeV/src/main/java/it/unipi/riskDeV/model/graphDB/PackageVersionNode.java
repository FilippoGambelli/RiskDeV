package it.unipi.riskDeV.model.graphDB;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;

import it.unipi.riskDeV.DTO.packageVersion.PublishedVersionDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Node("Version")
public class PackageVersionNode {
    @Id
    private String id;

    @Property("package_name")
    private String packageName;

    private String version;

    @Property("version_array")
    private List<Integer> versionArray;
    
    @Property("risk_score")
    private Double riskScore;

    private String documentation;

    @Property("requires_python")
    private String requiresPython;

    public PackageVersionNode(PublishedVersionDTO publishedVersionDTO, Double risk_score) {
        this.packageName = publishedVersionDTO.getPackageName();
        this.version = publishedVersionDTO.getVersion();
        this.versionArray = publishedVersionDTO.getVersionArray();
        this.documentation = publishedVersionDTO.getDocumentationURL();
        this.requiresPython = publishedVersionDTO.getRequiresPython();
        this.riskScore = risk_score;
    }
}