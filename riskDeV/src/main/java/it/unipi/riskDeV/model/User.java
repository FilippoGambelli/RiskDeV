package it.unipi.riskDeV.model;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Document(collection = "user")
public class User {
    @Id
    private String id;

    private String first_name;
    private String last_name;

    private String username;
    private String email;
    private String password;

    private List<String> project_ids = new ArrayList<>();
}
