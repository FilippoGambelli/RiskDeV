package it.unipi.riskDeV.APIClient;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OsvRequestDTO {
    
    // Jackson will use the field  "packageObj" how key
    // The API uses: { "package": { ... }, "version": "..." }
    @com.fasterxml.jackson.annotation.JsonProperty("package")
    private OsvPackage packageInfo;
    
    private String version;

    @Data
    @AllArgsConstructor
    public static class OsvPackage {
        private String name;
        private final String ecosystem = "PyPI";
    }
}