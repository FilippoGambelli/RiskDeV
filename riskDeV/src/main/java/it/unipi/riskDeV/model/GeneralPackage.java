package it.unipi.riskDeV.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

@Data
@Document(collection = "general_packages")
public class GeneralPackage {

    @Id
    private String id;
    private String package_name;
    private String author;
    private String author_email;
    private String description;
    private String package_url;
    private String summary;
    private String documentation;
    private String homepage;

    // List of versions using inner class
    private List<PackageVersionSummary> versions;

    @Data
    public static class PackageVersionSummary {

        private String version;
        private String version_id; 

    }

}