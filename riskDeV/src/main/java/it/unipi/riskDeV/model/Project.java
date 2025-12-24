package it.unipi.riskDeV.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

@Data
@Document(collection = "projects")
public class Project {

    @Id
    private String id;
    private String name;
    private String description;
    private String last_update;
    private String python_version;
    // List of used packages
    private List<ProjectPackageDependency> packages;

    @Data
    public static class ProjectPackageDependency {
        private String name;
        private String version;
    }
}