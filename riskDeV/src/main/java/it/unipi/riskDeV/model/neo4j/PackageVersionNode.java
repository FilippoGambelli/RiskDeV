package it.unipi.riskDeV.model.neo4j;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;

import it.unipi.riskDeV.model.PackageVersion;
import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
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

    // Relationship (:Version)-[:DEPENDS_ON]->(:Version)
    @Relationship(type = "DEPENDS_ON", direction = Relationship.Direction.OUTGOING)
    private Set<DependencyRelationship> dependencies;

    // Relationship (:Version)-[:AFFECTED_BY]->(:Vulnerability)
    @Relationship(type = "AFFECTED_BY", direction = Relationship.Direction.OUTGOING)
    private Set<VulnerabilityNode> vulnerabilities;

    public PackageVersionNode(PackageVersion packageVersion) {
        this.packageName = packageVersion.getPackageName();
        this.version = packageVersion.getVersion();
        this.versionArray = packageVersion.getVersionArray();
        this.requiresPython = packageVersion.getRequiresPython();
        this.documentation = packageVersion.getDocumentationURL();
    }
}