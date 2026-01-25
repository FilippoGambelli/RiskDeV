package it.unipi.riskDeV.model.neo4j;

import lombok.Data;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

//Represents the 'DEPENDS_ON' relationship between a Version and a Package.
@RelationshipProperties
@Data
public class DependencyRelationship {

    @Id
    @GeneratedValue
    private Long id;

    @TargetNode
    private PackageVersionNode targetVersion;
}