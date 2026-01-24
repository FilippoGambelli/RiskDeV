package it.unipi.riskDeV.model.neo4j;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.HashSet;
import java.util.Set;

@Data
@Node("Package")
public class PackageNode {

    @Id
    private String id;

    @Property("package_name")
    private String packageName;

    // Relationship: (:Package)-[:HAS_VERSION]->(:Version)
    @Relationship(type = "HAS_VERSION", direction = Relationship.Direction.OUTGOING)
    private Set<PackageVersionNode> versions = new HashSet<>();
}