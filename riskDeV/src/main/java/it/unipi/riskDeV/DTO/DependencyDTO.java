package it.unipi.riskDeV.DTO;

import lombok.Value;

@Value // Crea un oggetto immutabile (o usa @Data)
public class DependencyDTO {
    String targetPackage;  // Ex. "pandas"
    String targetVersion;  // Ex. "1.3.5" 
    String constraint;     // Ex. ">= 1.0" (took by relationship)
}