package it.unipi.riskDeV.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

@Data
@Document(collection = "projects")
public class Project {

    @Id
    private String id;

    private String description;

    @Field("last_update")
    private String lastUpdate;

    @Field("python_version")
    private String pythonVersion;

    private List<ProjectPackageDependency> packages;

    @Data
    public static class ProjectPackageDependency {
        private String name;
        private String version;
    }
}