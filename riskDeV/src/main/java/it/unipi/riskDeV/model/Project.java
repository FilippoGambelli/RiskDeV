package it.unipi.riskDeV.model;

import java.time.Instant;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.Data;

@Data
@Document(collection = "project")
public class Project {
    
    @Id
    private String id;

    private String name;

    private String description;

    private Collaborator admin;

    @Field("last_update")
    private Instant lastUpdate;

    @Field("python_version")
    private String pythonVersion;

    private List<ProjectPackage> packages;

    private List<Collaborator> collaborators;

    // Inner classes for ProjectPackage and Collaborator
    @Data
    public static class ProjectPackage {
        
        private String name;

        private String version;

        @Field("risk_score")
        private Double riskScore;

        @Field("vulnerabilities_count")
        private Integer vulnerabilitiesCount;
    }

    @Data 
    public static class Collaborator {

        private String username;

        private String email;
    }

}
