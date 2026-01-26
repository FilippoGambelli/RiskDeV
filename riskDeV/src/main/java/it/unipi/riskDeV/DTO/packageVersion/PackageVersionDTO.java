package it.unipi.riskDeV.DTO.packageVersion;

import io.swagger.v3.oas.annotations.media.Schema;
import it.unipi.riskDeV.model.PackageVersion;
import it.unipi.riskDeV.model.Constraints;
import it.unipi.riskDeV.model.EmbeddedVulnerability;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
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

    @Schema(description = "Version of the package (SemVer)", example = "10.10.0", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Version is required")
    @Size(max = 50, message = "Version string is too long (max 50 chars)")
    @Pattern(regexp = "^[a-zA-Z0-9.\\-+]+$", message = "Version contains invalid characters")
    private String version;

    @Schema(description = "Author of the package")
    private String author;

    @Schema(description = "Email of the author", example = "test@test.com")
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

    @Schema(description = "Calculated risk score based on vulnerabilities", example = "7.5", accessMode = Schema.AccessMode.READ_ONLY)
    private Double riskScore;

    @Schema(description = "List of known vulnerabilities associated with this version")
    @Valid 
    private List<EmbeddedVulnerabilityDTO> vulnerabilities;

    public PackageVersionDTO(PackageVersion model) {
        this.packageName = model.getPackageName();
        this.version = model.getVersion();
        this.uploadTime = model.getUploadTime();
        this.vulnerabilities = new ArrayList<>();
        for (EmbeddedVulnerability ev : model.getVulnerabilities()) {
            this.vulnerabilities.add(new EmbeddedVulnerabilityDTO(ev));
        }
        this.requiresPython = model.getRequiresPython();

        this.dependencies = new ArrayList<>();
        if(model.getDependencies() != null){
            for(Constraints dependency: model.getDependencies()) {
                this.dependencies.add(dependency.getFull());
            }
        }
        
        this.author = model.getAuthor();
        this.authorEmail = model.getAuthorEmail();
        this.description = model.getDescription();
        this.packageURL = model.getPackageURL();
        this.documentationURL = model.getDocumentationURL();
        this.riskScore = model.getRiskScore();
    }
}