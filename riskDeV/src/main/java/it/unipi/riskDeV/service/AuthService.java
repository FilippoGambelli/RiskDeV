package it.unipi.riskDeV.service;

import org.springframework.stereotype.Service;

import java.util.Map;

import org.springframework.security.crypto.password.PasswordEncoder;

import it.unipi.riskDeV.DTO.AuthResponseDTO;
import it.unipi.riskDeV.DTO.RegisterRequestDTO;
import it.unipi.riskDeV.common.DomainError;
import it.unipi.riskDeV.common.Result;
import it.unipi.riskDeV.DTO.LoginRequestDTO;
import it.unipi.riskDeV.model.User;
import it.unipi.riskDeV.model.neo4j.UserNode;
import it.unipi.riskDeV.repository.UserGraphRepository;
import it.unipi.riskDeV.repository.UserRepository;
import it.unipi.riskDeV.security.JwtUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    
    private final UserRepository userRepository;
    private final UserGraphRepository userGraphRepository;
    private final PasswordEncoder passwordEncoder;  
    private final JwtUtil jwtUtil;

    public Result<AuthResponseDTO> register(RegisterRequestDTO request) {

        log.info("Registering new user with email: {}", request.getEmail());
        
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed: email {} already exists.", request.getEmail());
            return new Result.Failure<>(new DomainError.AlreadyExists("Email already in use"));
        }

        if (userRepository.existsById(request.getUsername())) {
            log.warn("Registration failed: Username {} is already taken.", request.getUsername());
            return new Result.Failure<>(new DomainError.AlreadyExists("Username is already taken"));
        }

        User user = new User();
        user.setId(request.getUsername());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setRole("ROLE_USER");
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        try {
            userRepository.save(user);
            log.info("User saved in MongoDB: {}", user.getId());
        } catch (org.springframework.dao.DuplicateKeyException ex) {
            log.warn("Race condition: User {} already exists.", request.getUsername());
            return new Result.Failure<>(new DomainError.AlreadyExists("User already exists"));
        } catch (Exception e) {
            log.error("Failed to save user in MongoDB.", e);
            return new Result.Failure<>(new DomainError.SystemError("Error during registration. Please try again.", e));
        }
        
        try {   
            UserNode graphUser = new UserNode();
            graphUser.setId(user.getId());
            userGraphRepository.save(graphUser);
            log.info("User node created in Neo4j: {}", user.getId());
        } catch (Exception e) {
            log.error("Failed to save user in Neo4j. Rolling back MongoDB transaction.", e);
            try {
                userRepository.deleteById(user.getId());
                log.info("Rolled back MongoDB user: {}", user.getId());
            } catch (Exception ex) {
                log.error("Failed to rollback MongoDB user after Neo4j failure.", ex);
                // We could write in a simple log for manual check or implement an alert system 
                // to send us an alert.
            }
            return new Result.Failure<>(new DomainError.SystemError("Error during registration. Please try again.", e));
        }

        String token = jwtUtil.generateToken(user.getId(), user.getRole());
        return new Result.Success<>(new AuthResponseDTO(token, user.getId(), user.getEmail()));
    }

    public Result<AuthResponseDTO> login(LoginRequestDTO request) {

        log.info("Authenticating user.");

        return userRepository.findById(request.getUsername())
            .map(user -> {
                if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                    log.warn("Authentication failed for user: {}", request.getUsername());
                    return new Result.Failure<AuthResponseDTO>(new DomainError.InvalidCredentials("Invalid username or password"));
                }
                String token = jwtUtil.generateToken(user.getId(), user.getRole());
                log.info("User logged in successfully.");
                return new Result.Success<AuthResponseDTO>(new AuthResponseDTO(token, user.getId(), user.getEmail()));
            })
            .orElseGet(() -> {
                log.warn("Authentication failed for user: {}", request.getUsername());
                return new Result.Failure<AuthResponseDTO>(new DomainError.InvalidCredentials("Invalid username or password"));
            });
    }   

    public Result<Map<String, String>> deleteAccount(String userId) {

        if (!userRepository.existsById(userId)) {
            return new Result.Failure<>(new DomainError.NotFound("User not found"));
        }

        try {
            userRepository.deleteById(userId);
            log.info("User account with ID {} deleted successfully from Mongo.", userId);
        } catch (Exception e) {
            log.error("Failed to delete user account from MongoDB after Neo4j deletion.", e);
            return new Result.Failure<>(new DomainError.SystemError("System error: account not deleted.", e));
        }

        try {
            userGraphRepository.deleteById(userId);
            log.info("User node with ID {} deleted from Neo4j.", userId);
        } catch (Exception e) {
            log.error("Failed to delete user node from Neo4j.", e);
            // We could write in a simple log for manual check or implement a daemon to
            // retry the deletion later.
        }

        return new Result.Success<>(Map.of("message", "Account deleted successfully"));
    }
}
