package it.unipi.riskDeV.model;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import io.swagger.v3.oas.annotations.media.Schema;
import it.unipi.riskDeV.DTO.RegisterRequestDTO;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Document(collection = "user")
public class User {
    @Id
    private String id;

    @Indexed(unique = true) 
    @Field("username")
    private String username;

    @Schema(description = "First name of the user", example = "John")
    @Field("first_name")
    private String firstName;

    @Schema(description = "Last name of the user", example = "Doe")
    @Field("last_name")
    private String lastName;

    @Schema(description = "Email address of the user", example = "john.doe@example.com")
    private String email;

    @Schema(description = "Hashed password of the user", example = "$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36Z1Z6jI6j6b6j6b6j6b6j6")  
    private String password;

    @Schema(description = "Role of the user",  example = "ROLE_USER")
    private String role;
    
    @Schema(description = "List of project names associated with the user")
    @Field("project_names")
    private List<String> projectNames;

    public User(RegisterRequestDTO registerRequestDTO) {
        this.username = registerRequestDTO.getUsername();
        this.firstName = registerRequestDTO.getFirstName();
        this.lastName = registerRequestDTO.getLastName();
        this.email = registerRequestDTO.getEmail();
        this.password = null;
        this.projectNames = new ArrayList<>();
    }
}
