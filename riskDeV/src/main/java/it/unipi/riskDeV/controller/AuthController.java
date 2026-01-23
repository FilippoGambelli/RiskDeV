package it.unipi.riskDeV.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import it.unipi.riskDeV.service.AuthService;
import jakarta.validation.Valid;
import it.unipi.riskDeV.DTO.AuthResponseDTO;
import it.unipi.riskDeV.DTO.ErrorResponseDTO;
import it.unipi.riskDeV.DTO.LoginRequestDTO;
import it.unipi.riskDeV.DTO.RegisterRequestDTO;
import it.unipi.riskDeV.controller.util.RestResponseMapper;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;


@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication controller", description = "API for Authentication operations") 
@RequiredArgsConstructor
@Slf4j
@ApiResponses(value = {
    @ApiResponse(responseCode = "500", description = "Internal System Error",
        content = @Content(mediaType = "application/json", 
        schema = @Schema(implementation = ErrorResponseDTO.class)))
})
public class AuthController {
    
    private final AuthService authService;
    private final RestResponseMapper restResponseMapper;

    @PostMapping("/register")
    @Operation(
        summary = "Register", 
        description = "Creates a new user account and returns JWT token"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201", 
            description = "User created successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Invalid input data",
            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "409", 
            description = "Email or Username already exists",
            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
        )
    })
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequestDTO request) {
        log.info("Register attempt for: {}", request.getEmail());
        return restResponseMapper.map(authService.register(request), HttpStatus.CREATED);
    }

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Authenticates user and returns JWT token")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Authentication successful",
            content = @Content(schema = @Schema(implementation = AuthResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "401", 
            description = "Invalid credentials",
            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
        )
    })
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDTO request) {
        log.info("Login attempt for: {}", request.getUsername());
        return restResponseMapper.map(authService.login(request), HttpStatus.OK);
    }

    /*
    @DeleteMapping("/account")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete account", description = "Deletes the authenticated user's account")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Account deleted successfully",
            content = @Content(schema = @Schema(implementation = String.class))
        ),
        @ApiResponse(
            responseCode = "401", 
            description = "Unauthorized",
            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "User not found",
            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
        )
    })
    public ResponseEntity<?> deleteAccount(@AuthenticationPrincipal String userId) {
        log.info("Deleting account for user: {}", userId);
        return restResponseMapper.map(authService.deleteAccount(userId), HttpStatus.OK);
    }
    */

}
