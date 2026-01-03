package it.unipi.riskDeV.model.neo4j;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;
import java.util.List;

@Node("Project")
@Data
public class ProjectNode {

    @Id
    private String id;

    private String name;

    @Property("last_update")
    private String lastUpdate;

    @Relationship(type = "USES", direction = Relationship.Direction.OUTGOING)
    private List<PackageVersionNode> usedVersions;
}