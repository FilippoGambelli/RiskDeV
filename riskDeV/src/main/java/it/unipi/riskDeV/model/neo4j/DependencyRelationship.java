package it.unipi.riskDeV.model.neo4j;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.RelationshipId;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

//Represents the 'DEPENDS_ON' relationship between a Version and a Package.
@RelationshipProperties
@Data
public class DependencyRelationship {

    @RelationshipId
    private Long id;

    // Property stored in the edge of the graph
    private String constraint;

    @TargetNode
    private PackageVersionNode targetVersion;
}