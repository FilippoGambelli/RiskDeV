package it.unipi.riskDeV.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import it.unipi.riskDeV.DTO.project.ProjectCreationDTO;
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

    public Project(ProjectCreationDTO dto) {
        this.name = dto.name();
        this.description = dto.description();
        this.pythonVersion = dto.pythonVersion();
        this.lastUpdate = Instant.now();

        this.packages = new ArrayList<>();
        if (dto.packages() != null) {
            for (ProjectCreationDTO.PackageInput input : dto.packages()) {
                ProjectPackage pkg = new ProjectPackage();
                pkg.setName(input.name());
                pkg.setVersion(input.version());
                pkg.setRiskScore(null);
                pkg.setVulnerabilitiesCount(null);
                this.packages.add(pkg);
            }
        }

        this.collaborators = new ArrayList<>();
        this.admin = null;
    }

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
