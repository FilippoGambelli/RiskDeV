package it.unipi.riskDeV.APIClient;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Service
@Slf4j
public class PyPiApiClient {

    private final WebClient webClient;

    private static final String BASE_URL = "https://pypi.org/pypi";

    public PyPiApiClient(WebClient.Builder builder) {
        this.webClient = builder
                .baseUrl(BASE_URL)
                // Increase max memory size of the files
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                .build();
    }

    // Fetch latest version
    public Optional<PyPiResponseDTO> getPackageMetadata(String packageName) {
        return fetchFromPyPi("/" + packageName + "/json");
    }

    // Fetch specific version
    public Optional<PyPiResponseDTO> getPackageVersionMetadata(String packageName, String version) {
        return fetchFromPyPi("/" + packageName + "/" + version + "/json");
    }

    // Fetch complete metadata
    public Optional<PyPiResponseDTO> getFullPackageMetadata(String packageName) {
        return fetchFromPyPi("/" + packageName + "/json");
    }

    private Optional<PyPiResponseDTO> fetchFromPyPi(String uriPath) {
        try {
            PyPiResponseDTO response = webClient.get()
                    .uri(uriPath)
                    .retrieve()
                    // 404 Not Found
                    .onStatus(
                            status -> status.value() == 404,
                            clientResponse -> Mono.empty()
                    )
                    // Other errors
                    .onStatus(
                            status -> status.is4xxClientError() || status.is5xxServerError(),
                            clientResponse -> Mono.error(new RuntimeException("PyPI API Error: " + clientResponse.statusCode()))
                    )
                    .bodyToMono(PyPiResponseDTO.class)
                    .block(); // Sync. call

            return Optional.ofNullable(response);

        } catch (WebClientResponseException.NotFound e) {
            // Package doesn't exists
            return Optional.empty();
        } catch (Exception e) {
            // Other errors
            log.error("Error fetching data from PyPI for URI {}: {}", uriPath, e.getMessage());
            return Optional.empty();
        }
    }
}