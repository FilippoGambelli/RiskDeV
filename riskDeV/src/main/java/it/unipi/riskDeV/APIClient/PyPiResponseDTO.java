package it.unipi.riskDeV.APIClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true) // Ignores not mapped fields
public class PyPiResponseDTO {
    
    private Info info;

    private List<UrlEntry> urls;
    
    // The keys of the versions map are needed for ingestion service
    private Map<String, List<Object>> releases; 

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Info {
        private String name;
        private String version;
        private String author;
        
        @JsonProperty("author_email")
        private String authorEmail;
        
        private String summary;
        private String description;
        
        @JsonProperty("project_urls")
        private Map<String, String> projectUrls;
        
        @JsonProperty("requires_python")
        private String requiresPython;
        
        @JsonProperty("requires_dist")
        private List<String> requiresDist;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UrlEntry {
        @JsonProperty("upload_time_iso_8601")
        private String uploadTime;
    }

}