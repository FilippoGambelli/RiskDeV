package it.unipi.riskDeV.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.unipi.riskDeV.DTO.ErrorResponseDTO;
import it.unipi.riskDeV.DTO.UpdateProfileDTO;
import it.unipi.riskDeV.DTO.UserDTO;
import it.unipi.riskDeV.controller.util.RestResponseMapper;
import it.unipi.riskDeV.service.UserService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@Tag(name = "User controller", description = "Profile contoller")
@RequiredArgsConstructor
@ApiResponses(value = {
    @ApiResponse(
        responseCode = "500", 
        description = "Internal System Error",
        content = @Content(mediaType = "application/json", 
        schema = @Schema(implementation = ErrorResponseDTO.class)))
})
public class UserController {

    private final UserService userService;
    private final RestResponseMapper responseMapper;

    @GetMapping("/me")
    @Operation(
        summary = "Get current user profile",
        description = ""
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Get current user profile",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDTO.class))
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "Not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))
        )
    })
    public ResponseEntity<?> getMyProfile(@AuthenticationPrincipal String userId) {
        return responseMapper.map(userService.getProfile(userId), HttpStatus.OK);
    }
    
    @PatchMapping("/me")
    @Operation(
        summary = "Update user profile",
        description = ""
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Update user profile",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDTO.class))
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "Not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "409", 
            description = "Already exists",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))
        )
    })
    public ResponseEntity<?> updateProfile(@AuthenticationPrincipal String userId, @RequestBody UpdateProfileDTO dto) {
        return responseMapper.map(userService.updateProfile(userId, dto), HttpStatus.OK);
    }

    @DeleteMapping("/me")
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
        return responseMapper.map(userService.deleteUser(userId), HttpStatus.OK);
    }

    @GetMapping("/me/projects")
    @Operation(
        summary = "Get current user projects",
        description = ""
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Get current user projects",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "Not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))
        )
    })
    public ResponseEntity<?> getMyProjects(@AuthenticationPrincipal String userId) {
        return responseMapper.map(userService.getUserProjectNames(userId), HttpStatus.OK);
    }
}