package it.unipi.riskDeV.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AggreagationPackageDTO {
    private String packageName;
    private String version;
    private Integer count;
}
