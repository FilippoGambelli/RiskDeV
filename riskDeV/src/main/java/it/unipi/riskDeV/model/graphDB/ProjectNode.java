package it.unipi.riskDeV.model.graphDB;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

@Data
@Node("Project")
@EqualsAndHashCode(onlyExplicitlyIncluded = true) 
public class ProjectNode {

    @Id
    @EqualsAndHashCode.Include
    private String id;

    @ToString.Exclude
    private String name;
    
}