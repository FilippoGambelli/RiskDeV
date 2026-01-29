package it.unipi.riskDeV.DTO.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CentralityResultDTO {
    @Schema(description = "package name", example = "numpy")
    private String packageName;
    
    @Schema(description = "package version", example = "1.0")
    private String version;

    @Schema(description = "score", example = "8.9")
    private Double score;
}
