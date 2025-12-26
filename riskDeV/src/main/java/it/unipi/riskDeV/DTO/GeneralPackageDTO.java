package it.unipi.riskDeV.DTO;

import lombok.Data;
import java.util.List;
import java.util.stream.Collectors;

import io.swagger.v3.oas.annotations.media.Schema;
import it.unipi.riskDeV.model.GeneralPackage;

@Data
public class GeneralPackageDTO {

    @Schema(description = "Name of the package")
    private String packageName;

    @Schema(description = "Author of the package")
    private String author;

    @Schema(description = "Email of the author")
    private String authorEmail;

    @Schema(description = "Description of the package")
    private String description;

    @Schema(description = "The official URL of the package")
    private String packageURL;

    @Schema(description = "Summary of the package")
    private String summary;

    @Schema(description = "The official documentation URL of the package")
    private String documentationURL;

    @Schema(description = "The official homepage URL of the package")
    private String homepageURL;

    @Schema(description = "List of version numbers for the package, e.g., '1.0.0', '1.0.1'")
    private List<String> versions;

    /**
     * Constructor to convert a GeneralPackage model into a GeneralPackageDTO
     * @param model the GeneralPackage entity from the database
     */
    public GeneralPackageDTO(GeneralPackage model) {
        this.packageName = model.getId();
        this.author = model.getAuthor();
        this.authorEmail = model.getAuthorEmail();
        this.description = model.getDescription();
        this.packageURL = model.getPackageURL();
        this.summary = model.getSummary();
        this.documentationURL = model.getDocumentationURL();
        this.homepageURL = model.getHomepageURL();

        // Convert the list of PackageVersionSummary to a list of version strings
        this.versions = (model.getVersions() == null || model.getVersions().isEmpty()) 
                ? List.of() 
                : model.getVersions().stream()
                    .map(GeneralPackage.PackageVersionSummary::getVersion)
                    .collect(Collectors.toList());
    }
}