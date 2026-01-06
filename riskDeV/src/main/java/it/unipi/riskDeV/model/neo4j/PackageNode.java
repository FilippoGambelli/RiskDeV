package it.unipi.riskDeV.model.neo4j;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.HashSet;
import java.util.Set;

@Data
@Node("Package")
@EqualsAndHashCode(onlyExplicitlyIncluded = true) 
public class PackageNode {

    @Id
    @EqualsAndHashCode.Include
    private String id; // Name of the package
    // private String name;  --> I removed it because redundant if now id is defined as package name 

    // Relationship: (:Package)-[:HAS_VERSION]->(:Version)
    @Relationship(type = "HAS_VERSION", direction = Relationship.Direction.OUTGOING)
    @ToString.Exclude
    private Set<PackageVersionNode> versions = new HashSet<>();
}