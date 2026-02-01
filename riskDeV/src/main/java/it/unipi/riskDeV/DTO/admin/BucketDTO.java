package it.unipi.riskDeV.DTO.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BucketDTO {
    @Schema(description = "The risk score interval range", example = "2")
    private String riskInterval;
    @Schema(description = "The number of packages within this risk interval", example = "1500")
    private Integer count;
}
