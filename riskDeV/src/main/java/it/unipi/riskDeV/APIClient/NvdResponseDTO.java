package it.unipi.riskDeV.APIClient;

import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class NvdResponseDTO {
    private List<NvdVulnerabilityDTO> vulnerabilities;

    @Data
    public class NvdVulnerabilityDTO {
        private NvdCveDTO cve;
    }

    @Data
    public class NvdCveDTO {

        private String id;
        private String published;
        private Map<String, Object> metrics;
        private List<NvdDescriptionDTO> descriptions;

        public String getEnglishDescription() {
            if (descriptions == null) return null;

            return descriptions.stream()
                    .filter(d -> "en".equalsIgnoreCase(d.getLang()))
                    .map(NvdDescriptionDTO::getValue)
                    .findFirst()
                    .orElse(null);
        }
    }

    @Data
    public class NvdDescriptionDTO {
        private String lang;
        private String value;
    }
}
