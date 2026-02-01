package it.unipi.riskDeV.DTO.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateProfileDTO {
    
    @Schema(description = "New first name", example = "John")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    private String firstName;
    
    @Schema(description = "New last name", example = "Doe")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    private String lastName;
    
    @Schema(description = "New email", example = "johndoe@example.com")
    @Email(message = "Email must be valid")
    private String email;
    
    @Schema(description = "New password", example = "MySecureP@ssw0rd")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;
}