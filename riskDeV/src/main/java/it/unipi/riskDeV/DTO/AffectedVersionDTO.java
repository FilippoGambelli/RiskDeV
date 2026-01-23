package it.unipi.riskDeV.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AffectedVersionDTO {

    @Schema(description = "Name of the affected software package", example = "openssl")
    private String packageName;

    @Schema(description = "Affected software version", example = "1.1.1k")
    private String version;
}