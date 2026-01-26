package it.unipi.riskDeV.DTO.packageVersion;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Optional;

import io.swagger.v3.oas.annotations.media.Schema;
import it.unipi.riskDeV.model.PackageVersion;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateGeneralPackageDTO {

    @Schema(description = "Author of the package", example = "Travis E. Oliphant")
    @Size(max = 255, message = "Author name is too long")
    private Optional<String> author;

    @Schema(description = "Email of the author", example = "travis@numpy.org")
    @Email(message = "Invalid email format")
    private Optional<String> authorEmail;

    @Schema(description = "Description of the package")
    @Size(max = 2000, message = "Description is too long (max 2000 chars)")
    private Optional<String> description;

    @Schema(description = "The official URL of the package", example = "https://numpy.org/")
    private Optional<String> packageURL;

    @Schema(description = "The official documentation URL of the package")
    private Optional<String> documentationURL;

    public UpdateGeneralPackageDTO(PackageVersion model) {
        this.author = Optional.ofNullable(model.getAuthor());
        this.authorEmail = Optional.ofNullable(model.getAuthorEmail());
        this.description = Optional.ofNullable(model.getDescription());
        this.packageURL = Optional.ofNullable(model.getPackageURL());
        this.documentationURL = Optional.ofNullable(model.getDocumentationURL());
    }
}