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
import io.swagger.v3.oas.annotations.tags.Tag;
import it.unipi.riskDeV.DTO.UpdateProfileDTO;
import it.unipi.riskDeV.controller.util.RestResponseMapper;
import it.unipi.riskDeV.service.UserService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@Tag(name = "User Management", description = "Profile management, User Projects")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final RestResponseMapper responseMapper;

    @Operation(summary = "Get current user profile")
    @GetMapping("/me")
    public ResponseEntity<?> getMyProfile(@AuthenticationPrincipal String userId) {
        var result = userService.getProfile(userId);
        return responseMapper.map(result, HttpStatus.OK);
    }
    
    @PatchMapping("/me")
    public ResponseEntity<?> updateProfile(@AuthenticationPrincipal String userId, @RequestBody UpdateProfileDTO dto) {
        var result = userService.updateProfile(userId, dto);
        return responseMapper.map(result, HttpStatus.OK);
    }

    @DeleteMapping("/me")
    public ResponseEntity<?> deleteAccount(@AuthenticationPrincipal String userId) {
        var result = userService.deleteUser(userId);
        return responseMapper.map(result, HttpStatus.OK);
    }

    @GetMapping("/me/projects")
    public ResponseEntity<?> getMyProjects(@AuthenticationPrincipal String userId) {
        var result = userService.getUserProjectNames(userId);
        return responseMapper.map(result, HttpStatus.OK);
    }
}