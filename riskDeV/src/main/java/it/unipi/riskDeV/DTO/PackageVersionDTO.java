package it.unipi.riskDeV.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import it.unipi.riskDeV.model.PackageVersion;
import it.unipi.riskDeV.model.PackageVersion.EmbeddedVulnerability;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PackageVersionDTO {

    @Schema(description = "Name of the package", example = "numpy", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Package name is required")
    @Size(min = 2, max = 100, message = "Package name must be between 2 and 100 characters")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Package name contains invalid characters")
    private String packageName;

    @Schema(description = "Version of the package (SemVer)", example = "1.21.0", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Version is required")
    @Size(max = 50, message = "Version string is too long (max 50 chars)")
    @Pattern(regexp = "^[a-zA-Z0-9.\\-+]+$", message = "Version contains invalid characters")
    private String version;

    @Schema(description = "Author of the package")
    private String author;

    @Schema(description = "Email of the author")
    @Email(message = "Invalid email format")
    private String authorEmail;

    @Schema(description = "Description of the package")
    private String description;

    @Schema(description = "The official URL of the package")
    private String packageURL;

    @Schema(description = "The official documentation URL of the package")
    private String documentationURL;

    @Schema(description = "Upload time of the package (Server generated)", accessMode = Schema.AccessMode.READ_ONLY)
    private String uploadTime;

    @Schema(description = "Python version requirements", example = ">=3.6")
    @Size(max = 50, message = "Python requirements string is too long")
    private String requiresPython;

    @Schema(description = "List of package dependencies (raw strings)", example = "[\"pandas >= 1.0\", \"scipy\"]")
    private List<String> dependencies;

    @Schema(description = "Total number of vulnerabilities found", accessMode = Schema.AccessMode.READ_ONLY)
    private int vulnerabilityCount;

    @Schema(description = "List of known vulnerabilities associated with this version")
    @Valid 
    private List<EmbeddedVulnerability> vulnerabilities;

    public PackageVersionDTO(PackageVersion model) {
        this.packageName = model.getPackageName();
        this.version = model.getVersion();
        this.uploadTime = model.getUploadTime();
        this.vulnerabilities = model.getVulnerabilities();
        this.requiresPython = model.getRequiresPython();
        this.dependencies = model.getDependencies(); 
        this.vulnerabilityCount = (model.getVulnerabilities() != null) ? model.getVulnerabilities().size() : 0;
        this.author = model.getAuthor();
        this.authorEmail = model.getAuthorEmail();
        this.description = model.getDescription();
        this.packageURL = model.getPackageURL();
        this.documentationURL = model.getDocumentationURL();
    }
}