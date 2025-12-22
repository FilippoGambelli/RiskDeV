package it.unipi.riskDeV.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.tags.Tag;
import it.unipi.riskDeV.DTO.UserDTO;
import it.unipi.riskDeV.repository.UserRepository;

@RestController
@RequestMapping("/api/users")
@Tag(name = "User controller", description = "API for User operations")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserNameById(@PathVariable String id) {
        System.out.println("Searching user by id: " + id);
        return userRepository.findById(id)
            .map(user -> ResponseEntity.ok(
                new UserDTO(user.getFirst_name(), user.getLast_name())))
            .orElse(ResponseEntity.notFound().build());
    }
}