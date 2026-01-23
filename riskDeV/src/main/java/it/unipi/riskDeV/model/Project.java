package it.unipi.riskDeV.model;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.Data;

@Data
@Document(collection = "project")
public class Project {
    
    @Id
    private String name;

    private String description;

    @Field("admin_id")
    private String adminId;

    @Field("last_update")
    private LocalDateTime lastUpdate;

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

        private String id;

        private String username;

        private String email;
    }

}
