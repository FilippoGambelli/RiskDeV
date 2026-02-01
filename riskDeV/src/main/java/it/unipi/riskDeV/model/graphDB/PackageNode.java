package it.unipi.riskDeV.model.graphDB;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;

@Data
@Node("Package")
public class PackageNode {

    @Id
    private String id;

    @Property("package_name")
    private String packageName;
}