package it.unipi.riskDeV.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import it.unipi.riskDeV.model.Project;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class CollaboratorDTO {
     @Schema(
        description = "Unique username of the collaborator", 
        example = "john.doe",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Username cannot be empty")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @Schema(
        description = "Valid email address of the collaborator", 
        example = "john.doe@example.com",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Email cannot be empty")
    @Email(message = "Email format is invalid", regexp = "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$") 
    private String email;

    public static CollaboratorDTO fromEntity(Project.Collaborator collaborator) {
        if (collaborator == null) return null;
        return CollaboratorDTO.builder()
                .username(collaborator.getUsername())
                .email(collaborator.getEmail())
                .build();
    }
}
