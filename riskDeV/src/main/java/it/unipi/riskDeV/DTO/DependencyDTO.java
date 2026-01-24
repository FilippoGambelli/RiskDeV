package it.unipi.riskDeV.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DependencyDTO {
    String pkgName; // Ex. "pandas"
    String version;  // Ex. "1.3.5" 
    String constraint;     // Ex. ">= 1.0"
}