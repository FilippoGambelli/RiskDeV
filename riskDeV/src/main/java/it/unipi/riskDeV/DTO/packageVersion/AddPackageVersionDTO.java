package it.unipi.riskDeV.DTO.packageVersion;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddPackageVersionDTO {

    @Schema(description = "Name of the package", example = "numpy", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Package name is required")
    @Size(min = 2, max = 100, message = "Package name must be between 2 and 100 characters")
    private String packageName;

    @Schema(description = "Version of the package (SemVer)", example = "10.10.0", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Version is required")
    @Size(max = 50, message = "Version string is too long (max 50 chars)")
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

    @Schema(description = "upload time", example = "2011-10-23T21:40:37Z")
    private String uploadTime;

    @Schema(description = "Python version requirements", example = ">=3.6")
    private String requiresPython;

    @Schema(description = "List of package dependencies (raw strings)", example = "[\"pandas >= 1.0\", \"scipy\"]")
    private List<String> dependencies;

    @Schema(description = "List of known vulnerabilities associated with this version")
    @Valid 
    private List<EmbeddedVulnerabilityDTO> vulnerabilities;
}