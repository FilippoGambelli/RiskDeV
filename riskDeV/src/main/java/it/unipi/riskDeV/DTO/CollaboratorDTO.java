package it.unipi.riskDeV.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CollaboratorDTO {
    @NotBlank(message = "Username cannot be empty")
    @Schema(description = "Username of the collaborator to add/remove", example = "mario.rossi")
    private String username;
}
