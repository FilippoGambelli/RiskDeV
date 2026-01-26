package it.unipi.riskDeV.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import it.unipi.riskDeV.service.AuthService;
import it.unipi.riskDeV.util.ResultExecutor;
import jakarta.validation.Valid;
import it.unipi.riskDeV.DTO.ErrorResponseDTO;
import it.unipi.riskDeV.DTO.user.AuthResponseDTO;
import it.unipi.riskDeV.DTO.user.LoginRequestDTO;
import it.unipi.riskDeV.DTO.user.RegisterRequestDTO;
import it.unipi.riskDeV.results.RestResponseMapper;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;


@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication controller", description = "API for Authentication operations") 
@RequiredArgsConstructor
@ApiResponses(value = {
    @ApiResponse(
        responseCode = "500", 
        description = "Internal System Error",
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
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "409", 
            description = "Email or Username already exists",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))
        )
    })
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequestDTO request) {
        return restResponseMapper.map(ResultExecutor.execute(() -> (authService.register(request))), HttpStatus.CREATED);
    }

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Authenticates user and returns JWT token")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Authentication successful",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "401", 
            description = "Invalid credentials",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))
        )
    })
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDTO request) {
        return restResponseMapper.map(ResultExecutor.execute(() -> (authService.login(request))), HttpStatus.OK);
    }
}
