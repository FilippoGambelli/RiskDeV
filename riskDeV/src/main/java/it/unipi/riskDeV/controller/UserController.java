package it.unipi.riskDeV.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.unipi.riskDeV.DTO.ErrorResponseDTO;
import it.unipi.riskDeV.DTO.UpdateProfileDTO;
import it.unipi.riskDeV.DTO.UserDTO;
import it.unipi.riskDeV.controller.util.RestResponseMapper;
import it.unipi.riskDeV.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@Tag(name = "User controller", description = "Profile contoller")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@ApiResponses(value = {
    @ApiResponse(
        responseCode = "500", 
        description = "Internal System Error",
        content = @Content(
            mediaType = "application/json", 
            schema = @Schema(implementation = ErrorResponseDTO.class)
        )
    ),
    @ApiResponse(
        responseCode = "401", 
        description = "Unauthorized - Invalid or missing authentication token",
        content = @Content(
            mediaType = "application/json", 
            schema = @Schema(implementation = ErrorResponseDTO.class)
        )
    )
})
public class UserController {

    private final UserService userService;
    private final RestResponseMapper responseMapper;

    @GetMapping("/me")
    @Operation(
        summary = "Get current user profile",
        description = "Retrieves the profile information of the currently authenticated user including username, name, email, role, and associated projects"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Profile retrieved successfully",
            content = @Content(
                mediaType = "application/json", 
                schema = @Schema(implementation = UserDTO.class)
            )
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "User not found",
            content = @Content(
                mediaType = "application/json", 
                schema = @Schema(implementation = ErrorResponseDTO.class)
            )
        )
    })
    public ResponseEntity<?> getMyProfile(@AuthenticationPrincipal String username) {
        return responseMapper.map(userService.getProfile(username), HttpStatus.OK);
    }
    
    @PatchMapping("/me")
    @Operation(
        summary = "Update user profile",
        description = "Updates the current user's profile information. All fields are optional - only provided fields will be updated. Username must be unique if changed."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Profile updated successfully",
            content = @Content(
                mediaType = "application/json", 
                schema = @Schema(implementation = UserDTO.class)
            )
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Validation error - Invalid input data",
            content = @Content(
                mediaType = "application/json", 
                schema = @Schema(implementation = ErrorResponseDTO.class)
            )
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "User not found",
            content = @Content(
                mediaType = "application/json", 
                schema = @Schema(implementation = ErrorResponseDTO.class)
            )
        ),
        @ApiResponse(
            responseCode = "409", 
            description = "Conflict - Username already taken",
            content = @Content(
                mediaType = "application/json", 
                schema = @Schema(implementation = ErrorResponseDTO.class)
            )
        )
    })
    public ResponseEntity<?> updateProfile(@AuthenticationPrincipal String username, @RequestBody @Valid UpdateProfileDTO dto) {
        return responseMapper.map(userService.updateProfile(username, dto), HttpStatus.OK);
    }

    @DeleteMapping("/me")
    @Operation(
        summary = "Delete user account", 
        description = "Permanently deletes the authenticated user's account and all associated data. This action cannot be undone."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204", 
            description = "Account deleted successfully",
            content = @Content(
                mediaType = "text/plain",
                schema = @Schema(implementation = String.class)
            )
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "User not found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponseDTO.class)
            )
        )
    })
    public ResponseEntity<?> deleteAccount(@AuthenticationPrincipal String username) {
        return responseMapper.map(userService.deleteUser(username), HttpStatus.NO_CONTENT);
    }

    @GetMapping("/me/projects")
    @Operation(
        summary = "Get user's projects",
        description = "Retrieves a list of project names that the current user is involved in as either owner or collaborator"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Projects retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = String[].class)
            )
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "User not found",
            content = @Content(
                mediaType = "application/json", 
                schema = @Schema(implementation = ErrorResponseDTO.class)
            )
        )
    })
    public ResponseEntity<?> getMyProjects(@AuthenticationPrincipal String username) {
        return responseMapper.map(userService.getUserProjectNames(username), HttpStatus.OK);
    }
}