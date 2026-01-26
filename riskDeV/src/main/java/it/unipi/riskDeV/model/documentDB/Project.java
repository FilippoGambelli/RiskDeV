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
@Builder
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

    @Builder.Default
    private List<ProjectPackage> packages = new ArrayList<>();

    @Builder.Default
    private List<Collaborator> collaborators = new ArrayList<>();

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
