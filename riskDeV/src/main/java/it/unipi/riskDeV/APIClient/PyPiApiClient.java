package it.unipi.riskDeV.APIClient;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.client.RestTemplate;
import reactor.core.publisher.Mono;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class PyPiApiClient {

    private final WebClient webClient;
    private final RestTemplate restTemplate = new RestTemplate();
    private static final String BASE_URL = "https://pypi.org/pypi/";

    public PyPiApiClient(WebClient.Builder builder) {
        this.webClient = builder
                .baseUrl("https://pypi.org/pypi")
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

    // Fetch complete metadata (also the release list for injestion service)
    public Optional<PyPiResponseDTO> getFullPackageMetadata(String packageName) {
        try {
            return Optional.ofNullable(
                restTemplate.getForObject(BASE_URL + packageName + "/json", PyPiResponseDTO.class)
            );
        } catch (Exception e) {
            log.warn("Failed to fetch full metadata for package {}", packageName);
            return Optional.empty();
        }
    }

    private Optional<PyPiResponseDTO> fetchFromPyPi(String uriPath) {
        try {
            PyPiResponseDTO response = webClient.get()
                    .uri(uriPath)
                    .retrieve()
                    .onStatus(
                        status -> status.value() == 404,
                        clientResponse -> Mono.empty() // Optional.empty if the package doesn't exist
                    )
                    .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> clientResponse
                                .bodyToMono(String.class)
                                .flatMap(body -> Mono.error(
                                        new RuntimeException("PyPI API error " + clientResponse.statusCode())
                                ))
                    )
                    .bodyToMono(PyPiResponseDTO.class)
                    .block(); // It's synchronous

            return Optional.ofNullable(response);

        } catch (Exception e) {
            // We can't block the application if PiPy doen't work
            log.error("Error PyPi doesn't respond for download the requested package {}", e);
            return Optional.empty();
        }
    }
}
