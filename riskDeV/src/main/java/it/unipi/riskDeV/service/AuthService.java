package it.unipi.riskDeV.service;

import java.util.ArrayList;

import org.springframework.stereotype.Service;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import it.unipi.riskDeV.DTO.AuthResponseDTO;
import it.unipi.riskDeV.DTO.RegisterRequestDTO;
import it.unipi.riskDeV.DTO.LoginRequestDTO;
import it.unipi.riskDeV.model.User;
import it.unipi.riskDeV.model.neo4j.UserNode;
import it.unipi.riskDeV.repository.UserGraphRepository;
import it.unipi.riskDeV.repository.UserRepository;
import it.unipi.riskDeV.security.JwtUtil;
import it.unipi.riskDeV.exception.UserAlreadyExistsException;
import it.unipi.riskDeV.exception.ServiceException;

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

    public AuthResponseDTO register(RegisterRequestDTO request) {

        log.info("Registering new user with email: {}", request.getEmail());
        
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed: email {} already exists.", request.getEmail());
            throw new UserAlreadyExistsException("Email already in use");
        }

        if (userRepository.existsById(request.getUsername())) {
            log.warn("Registration failed: Username {} is already taken.", request.getUsername());
            throw new UserAlreadyExistsException("Username is already taken");
        }

        User user = new User();
        user.setId(request.getUsername());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setProject_ids(new ArrayList<>());

        User savedUser = userRepository.save(user);
        log.info("User saved in MongoDB: {}", savedUser.getId());

        try {
            UserNode graphUser = new UserNode(savedUser.getId(), null);
            userGraphRepository.save(graphUser);
            log.info("User node created in Neo4j: {}", savedUser.getId());
        } catch (Exception e) {
            log.error("Failed to save user in Neo4j. Rolling back MongoDB transaction.", e);
            userRepository.deleteById(savedUser.getId());
            throw new RuntimeException("Error during registration. Please try again.");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getEmail());
        return new AuthResponseDTO(token, user.getId(), user.getEmail());
    }

    public AuthResponseDTO login(LoginRequestDTO request) {

        log.info("Authenticating user.");

        User user = userRepository.findById(request.getUsername())
            .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Authentication failed for user: {}", request.getUsername());
            throw new BadCredentialsException("Invalid username or password");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getEmail());

        log.info("User logged in successfully.");
        return new AuthResponseDTO(token, user.getId(), user.getEmail());
    }   

    public void deleteAccount(String userId) {

        if (!userRepository.existsById(userId)) {
            throw new UsernameNotFoundException("User not found");
        }

        try {
            userGraphRepository.deleteById(userId);
            log.info("User node with ID {} deleted from Neo4j.", userId);
        } catch (Exception e) {
            log.error("Neo4j is unreachable. Aborting deletion.", e);
            throw new ServiceException("System error: cannot delete user graph data. Account not deleted.");
        }

        userRepository.deleteById(userId);
        log.info("User account with ID {} deleted successfully from Mongo.", userId);

    }
}
