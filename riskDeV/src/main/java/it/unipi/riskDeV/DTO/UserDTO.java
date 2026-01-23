package it.unipi.riskDeV.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import it.unipi.riskDeV.model.User;

import java.util.List;

public record UserDTO(

    @Schema(description = "Unique identifier of the user", example = "64b7f9c2e4b0c8a1d2f3e4b5")
    String id,

    @Schema(description = "Unique username", example = "mario.rossi")
    String username,

    @Schema(description = "User's first name", example = "Mario")
    String firstName,

    @Schema(description = "User's last name", example = "Rossi")
    String lastName,

    @Schema(description = "User's email address", example = "mario.rossi@example.com")
    String email,

    @Schema(description = "Assigned role", example = "ROLE_USER")
    String role,

    @Schema(description = "List of project names the user belongs to")
    List<String> projectNames
) {

    public static UserDTO fromEntity(User user) {
        if (user == null) return null;

        return new UserDTO(
            user.getId(),
            user.getUsername(), 
            user.getFirstName(),
            user.getLastName(),
            user.getEmail(),
            user.getRole(),
            user.getProjectNames() != null ? user.getProjectNames() : List.of()
        );
    }
    
}