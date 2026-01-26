package it.unipi.riskDeV.DTO.admin;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RiskAggregationDTO {
    private Integer totalPackages;
    private List<BucketDTO> buckets;
}