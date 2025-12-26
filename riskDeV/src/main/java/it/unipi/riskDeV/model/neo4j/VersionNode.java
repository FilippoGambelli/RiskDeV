package it.unipi.riskDeV.model.neo4j;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.List;

@Node("Version")
@Data
public class VersionNode {

    @Id
    private String id;

    private String version;
    
    @Property("upload_time")
    private String uploadTime;

    // Relationship: (Version)-[:AFFECTED_BY]->(Vulnerability)
    @Relationship(type = "AFFECTED_BY", direction = Relationship.Direction.OUTGOING)
    private List<VulnerabilityNode> vulnerabilities;

    // Relationship: (Version)-[:DEPENDS_ON]->(Package)
    @Relationship(type = "DEPENDS_ON", direction = Relationship.Direction.OUTGOING)
    private List<DependencyRelationship> dependencies;
}