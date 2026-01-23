package it.unipi.riskDeV.APIClient;

import it.unipi.riskDeV.model.PackageVersion.EmbeddedVulnerability;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class OsvApiClient {

    private final WebClient webClient;

    public OsvApiClient(WebClient.Builder builder) {
        this.webClient = builder
                .baseUrl("https://api.osv.dev/v1") 
                .build();
    }

    public List<EmbeddedVulnerability> getVulnerabilities(String packageName, String version) {
        log.debug("Fetching vulnerabilities from OSV (WebClient) for {} version {}", packageName, version);

        // Request body
        OsvRequestDTO requestBody = new OsvRequestDTO(
            new OsvRequestDTO.OsvPackage(packageName), 
            version
        );

        try {
            OsvResponseDTO response = webClient.post()
                    .uri("/query")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    // If errors 4xx/5xx: empty mono
                    .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> {
                            log.warn("OSV API returned error: {}", clientResponse.statusCode());
                            return Mono.empty();
                        }
                    )
                    .bodyToMono(OsvResponseDTO.class)
                    .block(); // It's synchronous the user is waiting his query

            if (response == null || response.getVulns() == null) {
                return new ArrayList<>();
            }

            return response.getVulns().stream()
                    .map(this::mapToEmbedded)
                    .toList();

        } catch (Exception e) {
            // Connession errors
            log.error("Failed to fetch vulnerabilities for {} {}: {}", packageName, version, e.getMessage());
            return new ArrayList<>();
        }
    }

    private EmbeddedVulnerability mapToEmbedded(OsvResponseDTO.OsvVulnerability osvVuln) {
        EmbeddedVulnerability emb = new EmbeddedVulnerability();
        
        // We need CVE-Id if exists
        String preferredId = osvVuln.getId();
        if (osvVuln.getAliases() != null) {
            preferredId = osvVuln.getAliases().stream()
                .filter(alias -> alias.startsWith("CVE-"))
                .findFirst()
                .orElse(osvVuln.getId());
        }
        
        emb.setCveId(preferredId);
        emb.setDetails(osvVuln.getDetails());
        
        if (osvVuln.getReferences() != null && !osvVuln.getReferences().isEmpty()) {
            emb.setLink(osvVuln.getReferences().get(0).getUrl());
        }

        return emb;
    }
}