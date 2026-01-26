package it.unipi.riskDeV.model.neo4j;

import org.springframework.data.annotation.Id;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
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
    @GeneratedValue
    private String id;

    @Property("package_name")
    private String packageName;

    private String version;

    @Property("version_array")
    private List<Integer> versionArray;
    
    @Property("risk_score")
    private Double riskScore;
    
    @Property("requires_python")
    private String requiresPython;

    private String documentation;

    public PackageVersionNode(PublishedVersionDTO publishedVersionDTO) {
        this.packageName = publishedVersionDTO.getPackageName();
        this.version = publishedVersionDTO.getVersion();
        this.versionArray = publishedVersionDTO.getVersionArray();
        this.requiresPython = publishedVersionDTO.getRequiresPython();
        this.documentation = publishedVersionDTO.getDocumentationURL();
    }
}