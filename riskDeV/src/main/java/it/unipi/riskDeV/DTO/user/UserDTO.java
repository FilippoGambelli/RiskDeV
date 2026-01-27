package it.unipi.riskDeV.DTO.user;

import io.swagger.v3.oas.annotations.media.Schema;
import it.unipi.riskDeV.model.documentDB.User;
import lombok.Value;

import java.util.List;

@Value
public class UserDTO {
    
    @Schema(description = "Unique username", example = "john.doe")
    String username;
    
    @Schema(description = "User's first name", example = "John")
    String firstName;
    
    @Schema(description = "User's last name", example = "Doe")
    String lastName;
    
    @Schema(description = "User's email address", example = "john.doe@example.com")
    String email;
    
    @Schema(description = "Assigned role", example = "ROLE_USER")
    String role;
    
    @Schema(description = "List of project names the user belongs to")
    List<String> projectNames;

    public UserDTO(User user) {
        this.username = user.getUsername();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.email = user.getEmail();
        this.role = user.getRole();
        this.projectNames = user.getProjectNames() != null ? user.getProjectNames() : List.of();
    }
}