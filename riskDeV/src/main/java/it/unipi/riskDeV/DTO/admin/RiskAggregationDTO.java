package it.unipi.riskDeV.DTO.admin;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RiskAggregationDTO {
    @Schema(description = "The total number of packages analyzed in the risk aggregation", example = "5000")
    private Integer totalPackages;
    @Schema(description = "A list of risk buckets containing the distribution of packages across different risk score intervals")
    private List<BucketDTO> buckets;
}