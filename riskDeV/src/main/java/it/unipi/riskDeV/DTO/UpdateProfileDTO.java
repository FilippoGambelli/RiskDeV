package it.unipi.riskDeV.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record UpdateProfileDTO(
    
    @Schema(description = "New username. Must be unique.", example = "super.mario")
    @Size(min = 3, max = 20)
    String username,
    
    @Schema(description = "New first name...", example = "Mario")
    @Size(min = 2, max = 50)
    String firstName,

    @Size(min = 2, max = 50)
    String lastName,

    @Size(min = 8)
    String password
) {}
