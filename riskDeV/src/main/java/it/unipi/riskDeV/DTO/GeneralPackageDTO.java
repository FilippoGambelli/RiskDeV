package it.unipi.riskDeV.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import it.unipi.riskDeV.model.Package;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeneralPackageDTO {

    @Schema(description = "Name of the package", example = "numpy", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Package name is required")
    @Size(min = 2, max = 100, message = "Package name must be between 2 and 100 characters")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Package name contains invalid characters")
    private String packageName;

    @Schema(description = "Author of the package", example = "Travis E. Oliphant")
    @Size(max = 255, message = "Author name is too long")
    private String author;

    @Schema(description = "Email of the author", example = "travis@numpy.org")
    @Email(message = "Invalid email format")
    private String authorEmail;

    @Schema(description = "Description of the package")
    @Size(max = 2000, message = "Description is too long (max 2000 chars)")
    private String description;

    @Schema(description = "The official URL of the package", example = "https://numpy.org/")
    private String packageURL;

    @Schema(description = "Summary of the package")
    @Size(max = 500, message = "Summary is too long (max 500 chars)")
    private String summary;

    @Schema(description = "The official documentation URL of the package")
    private String documentationURL;

    @Schema(description = "The official homepage URL of the package")
    private String homepageURL;

    @Schema(description = "List of version numbers (Read Only)", accessMode = Schema.AccessMode.READ_ONLY)
    private List<String> versions;

    public GeneralPackageDTO(Package model) {
        this.packageName = model.getId();
        this.author = model.getAuthor();
        this.authorEmail = model.getAuthorEmail();
        this.description = model.getDescription();
        this.packageURL = model.getPackageURL();
        this.summary = model.getSummary();
        this.documentationURL = model.getDocumentationURL();
        this.homepageURL = model.getHomepageURL();
        this.versions = model.getVersions();
    }
}