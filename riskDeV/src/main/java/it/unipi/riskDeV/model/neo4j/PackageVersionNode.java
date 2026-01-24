package it.unipi.riskDeV.model.neo4j;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;
import java.util.Set;

@Data
@Node("Version")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class PackageVersionNode {

    @Id 
    @GeneratedValue
    private Long id;

    @Property("package_name")
    @EqualsAndHashCode.Include    // In neo4j the version node is identify by packageName e version
    private String packageName;

    @EqualsAndHashCode.Include    // Second part of the key
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
    @ToString.Exclude
    private Set<VulnerabilityNode> vulnerabilities;
}