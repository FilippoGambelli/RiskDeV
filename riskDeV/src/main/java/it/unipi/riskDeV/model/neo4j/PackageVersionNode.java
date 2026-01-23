package it.unipi.riskDeV.model.neo4j;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Set;

@Data
@Node("Version")
@EqualsAndHashCode(of = "id")
public class PackageVersionNode {

    @Id
    private String id; // Corresponds to "package_name version"

    private String version;

    private Boolean isStub;

    // Essential for queries comparing package versions
    private Integer major;
    private Integer minor;
    private Integer patch;

    // Relationship (:Version)-[:DEPENDS_ON]->(:Version)
    @Relationship(type = "DEPENDS_ON", direction = Relationship.Direction.OUTGOING)
    private Set<DependencyRelationship> dependencies;

    // Relationship (:Version)-[:AFFECTED_BY]->(:Vulnerability)
    @Relationship(type = "AFFECTED_BY", direction = Relationship.Direction.OUTGOING)
    @ToString.Exclude
    private Set<VulnerabilityNode> vulnerabilities;
}