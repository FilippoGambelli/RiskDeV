package it.unipi.riskDeV.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.unipi.riskDeV.DTO.CollaboratorDTO;
import it.unipi.riskDeV.DTO.ErrorResponseDTO;
import it.unipi.riskDeV.DTO.InstalledPackageDTO;
import it.unipi.riskDeV.DTO.ProjectDTO;
import it.unipi.riskDeV.controller.util.RestResponseMapper;
import it.unipi.riskDeV.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;

@RestController
@RequestMapping("/api/projects")
@Tag(name = "Projects controller", description = "API for Projects operations")
@RequiredArgsConstructor
@Slf4j
@ApiResponses(value = {
    @ApiResponse(responseCode = "500", description = "Internal System Error",
        content = @Content(mediaType = "application/json", 
        schema = @Schema(implementation = ErrorResponseDTO.class))),
    @ApiResponse(responseCode = "401", description = "Unauthorized",
        content = @Content(mediaType = "application/json", 
        schema = @Schema(implementation = ErrorResponseDTO.class)))
})
public class ProjectController {
    
    private final ProjectService projectService;
    private final RestResponseMapper restResponseMapper;

    /*
    @GetMapping("/")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "List of all the projects in which the user is a collaborator",
            description = "Fetches a list of all the projects in which the user is a collaborator.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Projects retrieved successfully",
            content = @Content(mediaType = "application/json", 
            array = @ArraySchema(schema = @Schema(implementation = String.class, description = "Project ID"))))
    })
    public ResponseEntity<?> getAllUserProjects(@AuthenticationPrincipal String userId) {
        log.info("Searching all the projects in which the user is a collaborator.");
        return restResponseMapper.map(projectService.getAllUserProjects(userId), HttpStatus.OK);
    }
    */

    @PostMapping("/")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Insert a new project",
            description = "Insert a new project for the current user.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Project created successfully",
            content = @Content(mediaType = "application/json", 
            schema = @Schema(implementation = String.class, description = "The ID of the created project")))
    })
    public ResponseEntity<?> insertProject(@AuthenticationPrincipal String userId, @Valid @RequestBody ProjectDTO projectDTO) {
        log.info("Inserting a new project for the current user.");
        return restResponseMapper.map(projectService.insertProject(userId, projectDTO), HttpStatus.CREATED);
    }

    @DeleteMapping("/{projectId}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete a project",
            description = "Deletes a project for the current user.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Project deleted successfully",
            content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "404", description = "Project not found",
            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "403", description = "Access Denied",
            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<?> deleteProject(@AuthenticationPrincipal String userId, @PathVariable String projectId) {
        log.info("Deleting a project for the current user.");
        return restResponseMapper.map(projectService.deleteProject(userId, projectId), HttpStatus.OK);
    }

    @PutMapping("/{projectId}/packages")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update project packages",
            description = "Updates the packages of a project for the current user.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Packages updated successfully",
            content = @Content(schema = @Schema(implementation = String.class, description = "Project ID"))),
        @ApiResponse(responseCode = "404", description = "Project not found",
            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "403", description = "Access Denied",
            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid package data",
            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<?> updateProjectPackages(@AuthenticationPrincipal String userId, @PathVariable String projectId, @Valid @RequestBody List<InstalledPackageDTO> packages) {
        log.info("Updating packages of a project for the current user.");
        return restResponseMapper.map(projectService.updateProjectPackages(userId, projectId, packages), HttpStatus.OK);
    }

    @DeleteMapping("/{projectId}/packages")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Remove packages from project",    
            description = "Removes packages from a project for the current user.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Packages removed successfully",
            content = @Content(schema = @Schema(implementation = String.class, description = "Project ID"))),
        @ApiResponse(responseCode = "404", description = "Project not found",
            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "403", description = "Access Denied",
            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<?> removePackagesFromProject(@AuthenticationPrincipal String userId, @PathVariable String projectId, @Valid @RequestBody List<InstalledPackageDTO> packages) {
        log.info("Removing packages from a project for the current user.");
        return restResponseMapper.map(projectService.removePackagesFromProject(userId, projectId, packages), HttpStatus.OK);
    }

    @PutMapping("/{projectId}/users")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Add collaborator to project",
            description = "Adds a collaborator to a project for the current user.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Collaborator added successfully",
            content = @Content(schema = @Schema(implementation = String.class, example = "Collaborator added."))),
        @ApiResponse(responseCode = "404", description = "Project or User not found",
            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "403", description = "Access Denied",
            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid Operation",
            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<?> addCollaboratorToProject(@AuthenticationPrincipal String userId, @PathVariable String projectId, @Valid @RequestBody CollaboratorDTO collaborator) {
        log.info("Adding a collaborator to a project for the current user.");
        return restResponseMapper.map(projectService.addCollaboratorToProject(userId, projectId, collaborator.getUsername()), HttpStatus.OK);
    }

    @GetMapping("/{projectId}/users")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get collaborators of project",
            description = "Fetches the list of collaborators of a project for the current user.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "List of collaborators retrieved",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = String.class, description = "Username")))),
        @ApiResponse(responseCode = "404", description = "Project not found",
            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "403", description = "Access Denied",
            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<?> getCollaboratorsOfProject(@AuthenticationPrincipal String userId, @PathVariable String projectId) {
        log.info("Fetching collaborators of a project for the current user.");
        return restResponseMapper.map(projectService.getProjectCollaborators(userId, projectId), HttpStatus.OK);
    }   
    
    @DeleteMapping("/{projectId}/users")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Remove collaborator from project",
            description = "Removes a collaborator from a project for the current user.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Collaborator removed successfully",
            content = @Content(schema = @Schema(implementation = String.class, description = "Removed Username"))),
        @ApiResponse(responseCode = "404", description = "Project not found",
            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "403", description = "Access Denied",
            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<?> removeCollaboratorFromProject(@AuthenticationPrincipal String userId, @PathVariable String projectId, @Valid @RequestBody CollaboratorDTO collaborator) {
        log.info("Removing a collaborator from a project for the current user.");
        return restResponseMapper.map(projectService.removeCollaboratorFromProject(userId, projectId, collaborator.getUsername()), HttpStatus.OK);
    }
    
}
