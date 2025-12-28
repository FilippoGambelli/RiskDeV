package it.unipi.riskDeV.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import it.unipi.riskDeV.service.AuthService;
import jakarta.validation.Valid;
import it.unipi.riskDeV.DTO.AuthResponse;
import it.unipi.riskDeV.DTO.LoginRequest;
import it.unipi.riskDeV.DTO.RegisterRequest;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder; 


@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication controller", description = "API for Authentication operations") 
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    
    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register", description = "Creates a new user account and returns JWT token")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        log.info("Register attempt for: {}", request.getEmail());
        return authService.register(request);
    }

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Authenticates user and returns JWT token")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        log.info("Login attempt for: {}", request.getUsername());
        return authService.login(request);
    }

    @DeleteMapping("/account")
    @SecurityRequirement(name = "bearerAuth")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete account", description = "Deletes the authenticated user's account")
    public void deleteAccount() {
        String currentUserId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        log.info("Deleting account for user: {}", currentUserId);
        authService.deleteAccount(currentUserId);
    }

}
