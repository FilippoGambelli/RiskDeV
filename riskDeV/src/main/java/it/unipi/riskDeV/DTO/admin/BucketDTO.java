package it.unipi.riskDeV.DTO.admin;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BucketDTO {
    private String riskInterval;
    private int count;
}
