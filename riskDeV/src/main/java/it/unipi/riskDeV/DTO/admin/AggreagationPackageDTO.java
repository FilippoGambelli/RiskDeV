package it.unipi.riskDeV.DTO.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AggreagationPackageDTO {
    @Schema(description = "The name of the package", example = "lodash")
    private String packageName;
    @Schema(description = "The version of the package", example = "4.17.21")
    private String version;
    @Schema(description = "The number of projects or dependencies that use this package", example = "1250")
    private Integer count;
}
