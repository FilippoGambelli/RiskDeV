package it.unipi.riskDeV.model.graphDB;

import lombok.Data;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

@Data
@Node("Project")
public class ProjectNode {

    @Id
    private String id;

    private String name;
    
}