package it.unipi.riskDeV.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InstalledPackageDTO {
    @Schema(description = "Name of the package", example = "numpy")
    @NotBlank(message = "Package name must not be blank")
    private String name;

    @Schema(description = "Version of the package", example = "1.21.0")
    @NotBlank(message = "Package version must not be blank")
    private String version;
}
