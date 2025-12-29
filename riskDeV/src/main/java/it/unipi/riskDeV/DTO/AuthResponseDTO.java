package it.unipi.riskDeV.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponseDTO {

    @Schema(description = "JWT token for authenticated access")
    private String token;

    @Schema(description = "ID of the authenticated user")
    private String id;

    @Schema(description = "Email of the authenticated user")
    private String email;

}

