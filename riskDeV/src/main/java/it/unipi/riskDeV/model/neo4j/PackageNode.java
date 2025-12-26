package it.unipi.riskDeV.model.neo4j;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.List;

@Node("Package")
@Data
public class PackageNode {

    @Id
    private String id;

    private String name;
    private String author;
    private String url;

    // Relationship: (Package)-[:HAS_VERSION]->(Version)
    @Relationship(type = "HAS_VERSION", direction = Relationship.Direction.OUTGOING)
    private List<VersionNode> versions;
}