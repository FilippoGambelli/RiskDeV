package it.unipi.riskDeV.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Standard error response container")
public record ErrorResponseDTO(
    @Schema(description = "Error description", example = "Error message details")
    String message,
    
    @Schema(description = "Timestamp of the error")
    LocalDateTime timestamp
) {
    public static ErrorResponseDTO withMessage(String message) {
        return new ErrorResponseDTO(message, LocalDateTime.now());
    }
}
