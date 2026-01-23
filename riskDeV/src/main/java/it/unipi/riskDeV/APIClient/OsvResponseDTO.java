package it.unipi.riskDeV.APIClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OsvResponseDTO {
    
    private List<OsvVulnerability> vulns;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OsvVulnerability {
        private String id; // <---  ID OSV (ex. PYSEC-...)
        private String details;
        private List<String> aliases;
        private List<OsvReference> references;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OsvReference {
        private String type;
        private String url;
    }
}