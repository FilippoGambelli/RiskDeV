package it.unipi.riskDeV.model.graphDB;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Data
@Node("Project")
@EqualsAndHashCode(onlyExplicitlyIncluded = true) 
public class ProjectNode {

    @Id
    @EqualsAndHashCode.Include
    private String id;

    @ToString.Exclude
    private String name;

    @ToString.Exclude
    private String description;

    @ToString.Exclude
    private String pythonVersion;

    @ToString.Exclude
    private Instant lastUpdate;

    @Relationship(type = "OWNS", direction = Relationship.Direction.INCOMING)
    @ToString.Exclude
    private UserNode owner;

    @Relationship(type = "WORKS_ON", direction = Relationship.Direction.INCOMING)
    @ToString.Exclude
    private Set<UserNode> members = new HashSet<>();

    @Relationship(type = "USES", direction = Relationship.Direction.OUTGOING)
    @ToString.Exclude
    private Set<PackageVersionNode> dependencies = new HashSet<>();
    
}