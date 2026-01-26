package it.unipi.riskDeV.model.graphDB;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.HashSet;
import java.util.Set;

@Data
@Node("User")
@EqualsAndHashCode(onlyExplicitlyIncluded = true) 
public class UserNode {

    @Id
    @EqualsAndHashCode.Include 
    private String id;

    @Relationship(type = "OWNS", direction = Relationship.Direction.OUTGOING)
    @ToString.Exclude
    private Set<ProjectNode> ownedProjects = new HashSet<>();

    @Relationship(type = "WORKS_ON", direction = Relationship.Direction.OUTGOING)
    @ToString.Exclude 
    private Set<ProjectNode> projects = new HashSet<>();

}