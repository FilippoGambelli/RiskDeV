package it.unipi.riskDeV.model.neo4j;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;
import java.util.List;

@Node("User")
@Data
@AllArgsConstructor
public class UserNode {

    @Id
    private String id;

    // Relationship: (User)-[:OWNS_PROJECT]->(Project)
    @Relationship(type = "OWNS_PROJECT", direction = Relationship.Direction.OUTGOING)
    private List<ProjectNode> projects;
}