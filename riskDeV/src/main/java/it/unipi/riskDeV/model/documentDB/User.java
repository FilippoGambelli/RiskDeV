package it.unipi.riskDeV.model.documentDB;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import it.unipi.riskDeV.DTO.user.RegisterRequestDTO;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Document(collection = "user")
public class User {
    @Id
    private String id;

    private String username;

    @Field("first_name")
    private String firstName;

    @Field("last_name")
    private String lastName;

    private String email;

    private String password;

    private String role;
    
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
