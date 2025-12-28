package it.unipi.riskDeV.model.neo4j;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import lombok.Data;

@Node("Version")
@Data
public class VersionNode {

    @Id
    private String id; // Es: "requests 2.28.1"

    @Property("version")
    private String versionNumber;

    @Property("major")
    private Integer major;

    @Property("minor")
    private Integer minor;

    @Property("patch")
    private Integer patch;
}