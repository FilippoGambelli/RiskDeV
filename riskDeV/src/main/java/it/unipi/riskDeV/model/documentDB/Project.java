package it.unipi.riskDeV.model.documentDB;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
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

    private List<ProjectPackage> packages = new ArrayList<>();

    private List<Collaborator> collaborators = new ArrayList<>();

    public Project(String name, String description, String pythonVersion, Collaborator admin, List<ProjectPackage> packages) {
        this.name = name;
        this.description = description;
        this.pythonVersion = pythonVersion;
        this.admin = admin;
        this.packages = packages != null ? packages : new ArrayList<>();
        this.lastUpdate = Instant.now();
        this.collaborators = new ArrayList<>();
        if (admin != null) {
            this.collaborators.add(admin);
        }
    }

    // Inner classes for ProjectPackage and Collaborator
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProjectPackage {
        
        private String name;

        private String version;

        @Field("risk_score")
        private Double riskScore;

        @Field("vulnerabilities_count")
        private Integer vulnerabilitiesCount;

        public ProjectPackage(String name, String version) {
            this.name = name;
            this.version = version;
            this.riskScore = 0.0;
            this.vulnerabilitiesCount = 0;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Collaborator {

        private String username;

        private String email;
    }

}
