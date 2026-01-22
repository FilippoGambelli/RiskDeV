package it.unipi.riskDeV.APIClient;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import it.unipi.riskDeV.exception.NvdApiException;
import reactor.core.publisher.Mono;

@Service
public class ApiClient {

    private final WebClient webClient;

    public ApiClient(WebClient.Builder builder) {
        this.webClient = builder
                .baseUrl("https://services.nvd.nist.gov/rest/json/cves/2.0")
                .build();
    }

    public Optional<NvdResponseDTO> getCveById(String cveId) {

        try {
            NvdResponseDTO response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("cveId", cveId)
                            .build())
                    .retrieve()
                    .onStatus(
                        status -> status.value() == 404,
                        clientResponse -> Mono.empty()
                    )
                    .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> clientResponse
                                .bodyToMono(String.class)
                                .flatMap(body -> Mono.error(
                                        new NvdApiException(
                                                "NVD API error " +
                                                clientResponse.statusCode() +
                                                " - " + body
                                        )
                                ))
                    )
                    .bodyToMono(NvdResponseDTO.class)
                    .block();

            return Optional.ofNullable(response);

        } catch (NvdApiException e) {
            throw e;
        } catch (Exception e) {
            throw new NvdApiException("NVD API call failed", e);
        }
    }
}