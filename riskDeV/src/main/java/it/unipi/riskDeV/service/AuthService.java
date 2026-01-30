package it.unipi.riskDeV.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;

import org.springframework.security.crypto.password.PasswordEncoder;

import it.unipi.riskDeV.DTO.user.AuthResponseDTO;
import it.unipi.riskDeV.DTO.user.LoginRequestDTO;
import it.unipi.riskDeV.DTO.user.RegisterRequestDTO;
import it.unipi.riskDeV.model.documentDB.User;
import it.unipi.riskDeV.repository.documentDB.UserRepository;
import it.unipi.riskDeV.results.DomainError;
import it.unipi.riskDeV.results.Result;
import it.unipi.riskDeV.security.JwtUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;  
    private final JwtUtil jwtUtil;

    public Result<AuthResponseDTO> register(RegisterRequestDTO request) {

        log.info("Registering new user with username: {}", request.getUsername());

        if (userRepository.existsByUsername(request.getUsername())) {
            return new Result.Failure<>(new DomainError.AlreadyExists("Username is already taken"));
        }
        
        if (userRepository.existsByEmail(request.getEmail())) {
            return new Result.Failure<>(new DomainError.AlreadyExists("Email already in use"));
        }
        
        User user = new User(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole("ROLE_USER");
        user.setProjectNames(new ArrayList<>());

        try {
            User savedUser = userRepository.save(user);
            log.info("User saved in MongoDB: {}", savedUser.getUsername());

            String token = jwtUtil.generateToken(savedUser.getId(), savedUser.getUsername(), savedUser.getRole());

            return new Result.Success<>(new AuthResponseDTO(token, savedUser.getUsername(), savedUser.getEmail()));
        } catch (Exception e) {
            log.error("Failed to save user in Mongo.", e);
            return new Result.Failure<>(new DomainError.SystemError(e));
        }
    }

    public Result<AuthResponseDTO> login(LoginRequestDTO request) {

        log.info("Authenticating user with username {}.", request.getUsername());

        var userOpt = userRepository.findByUsername(request.getUsername());
        if (userOpt.isEmpty()) {
            return new Result.Failure<>(new DomainError.InvalidCredentials("Invalid username or password"));
        }

        User user = userOpt.get();
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return new Result.Failure<>(new DomainError.InvalidCredentials("Invalid username or password"));
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        log.info("User {} logged in successfully.", user.getId());

        return new Result.Success<>(new AuthResponseDTO(token, user.getUsername(), user.getEmail()));
    }
}
