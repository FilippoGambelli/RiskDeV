package it.unipi.riskDeV.DTO.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageRankResultDTO {
    @Schema(description = "package name", example = "numpy")
    private String package_name;

    @Schema(description = "package version", example = "1.0")
    private String version;

    @Schema(description = "score", example = "65.59")
    private Double score;
}
