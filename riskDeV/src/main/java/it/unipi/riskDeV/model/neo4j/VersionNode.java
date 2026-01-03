package it.unipi.riskDeV.model.neo4j;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;
import lombok.Data;
import java.util.List;

@Data
@Node("Version")
public class VersionNode {

    @Id
    private String id; // Corresponds to "package_name version"

    private String version;

    // Essential for queries comparing package versions
    private Integer major;
    private Integer minor;
    private Integer patch;

    // Relationship (:Version)-[:DEPENDS_ON]->(:Version)
    @Relationship(type = "DEPENDS_ON", direction = Relationship.Direction.OUTGOING)
    private List<VersionNode> dependencies;

    // Relationship (:Version)-[:AFFECTED_BY]->(:Vulnerability)
    @Relationship(type = "AFFECTED_BY", direction = Relationship.Direction.OUTGOING)
    private List<VulnerabilityNode> vulnerabilities;
}