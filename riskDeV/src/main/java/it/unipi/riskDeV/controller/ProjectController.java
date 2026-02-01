package it.unipi.riskDeV.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.unipi.riskDeV.DTO.ErrorResponseDTO;
import it.unipi.riskDeV.DTO.MessageResponseDTO;
import it.unipi.riskDeV.DTO.project.CollaboratorDTO;
import it.unipi.riskDeV.DTO.project.InstalledPackageDTO;
import it.unipi.riskDeV.DTO.project.ProjectCreationDTO;
import it.unipi.riskDeV.DTO.project.ProjectDTO;
import it.unipi.riskDeV.results.RestResponseMapper;
import it.unipi.riskDeV.service.ProjectService;
import it.unipi.riskDeV.util.ResultExecutor;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
@SecurityRequirement(name = "bearerAuth")
@ApiResponses(value = {
    @ApiResponse(
        responseCode = "500", 
        description = "Internal System Error",
        content = @Content(mediaType = "application/json", 
        schema = @Schema(implementation = ErrorResponseDTO.class))),
    @ApiResponse(
        responseCode = "401", 
        description = "Unauthorized",
        content = @Content(mediaType = "application/json", 
        schema = @Schema(implementation = ErrorResponseDTO.class)))
})
public class ProjectController {
    
    private final ProjectService projectService;
    private final RestResponseMapper restResponseMapper;

    @PostMapping("/")
    @Operation(summary = "Create a new project", description = "Creates a new project owned by the authenticated user.")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201", 
            description = "Project created successfully",
            content = @Content(mediaType = "application/json", 
            schema = @Schema(implementation = ProjectDTO.class))),
        @ApiResponse(
            responseCode = "400", 
            description = "Validation Error",
            content = @Content(mediaType = "application/json", 
            schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<?> insertProject(@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "\"Project creation payload\", required = true") @RequestBody @Valid ProjectCreationDTO projectCreationDTO) {
        return restResponseMapper.map(ResultExecutor.execute(() -> (projectService.addProject(projectCreationDTO))), HttpStatus.CREATED);
    }

    @GetMapping("/{projectName}")
    @Operation(summary = "Get project details", description = "Retrieves full details of a specific project.")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Project found",
            content = @Content(mediaType = "application/json", 
            schema = @Schema(implementation = ProjectDTO.class))),
        @ApiResponse(
            responseCode = "404", 
            description = "Project not found",
            content = @Content(mediaType = "application/json", 
            schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(
            responseCode = "403", 
            description = "Access Denied",
            content = @Content(mediaType = "application/json", 
            schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<?> getProjectDetails(@Parameter(description = "Unique name of the project", example = "RiskAnalysis_AI") @PathVariable String projectName) {
        return restResponseMapper.map(ResultExecutor.execute(() -> (projectService.getProject(projectName))), HttpStatus.OK);
    }

    @DeleteMapping("/{projectName}")
    @Operation(summary = "Delete a project", description = "Permanently deletes a project. Only the Owner can perform this action.")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204", 
            description = "Project deleted successfully",
            content = @Content(mediaType = "application/json", 
            schema = @Schema(implementation = MessageResponseDTO.class))),
        @ApiResponse(
            responseCode = "403", 
            description = "Access Denied",
            content = @Content(mediaType = "application/json", 
            schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(
            responseCode = "404", 
            description = "Project not found",
            content = @Content(mediaType = "application/json", 
            schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<?> deleteProject(@Parameter(description = "Name of the project to delete", example = "RiskAnalysis_AI") @PathVariable String projectName) {
        return restResponseMapper.map(ResultExecutor.execute(() -> (projectService.deleteProject(projectName))), HttpStatus.OK);
    }

    @PutMapping("/{projectName}/packages")
    @Operation(summary = "Upsert packages", description = "Adds new packages or updates versions of existing ones.")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Packages updated successfully",
            content = @Content(mediaType = "application/json", 
            schema = @Schema(implementation = MessageResponseDTO.class))),
        @ApiResponse(
            responseCode = "400", 
            description = "Invalid package list",
            content = @Content(mediaType = "application/json", 
            schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(
            responseCode = "403", 
            description = "Access Denied",
            content = @Content(mediaType = "application/json", 
            schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(
            responseCode = "404", 
            description = "Project not found",
            content = @Content(mediaType = "application/json", 
            schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<?> updateProjectPackages(
            @Parameter(description = "Project name", example = "RiskAnalysis_AI") @PathVariable String projectName, 
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "List of packages to add/update")
            @Valid @RequestBody List<InstalledPackageDTO> packages) {
        return restResponseMapper.map(ResultExecutor.execute(() -> (projectService.updateProjectPackages(projectName, packages))), HttpStatus.OK);
    }

    @DeleteMapping("/{projectName}/packages")
    @Operation(summary = "Remove packages", description = "Removes packages from the project configuration.")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204", 
            description = "Packages removed successfully",
            content = @Content(mediaType = "application/json", 
            schema = @Schema(implementation = MessageResponseDTO.class))),
        @ApiResponse(
            responseCode = "403", 
            description = "Access Denied",
            content = @Content(mediaType = "application/json", 
            schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(
            responseCode = "404", 
            description = "Project not found",
            content = @Content(mediaType = "application/json", 
            schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<?> removePackagesFromProject(
            @Parameter(description = "Project name", example = "RiskAnalysis_AI") @PathVariable String projectName, 
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "List of packages to remove")
            @Valid @RequestBody List<String> packages) {
        return restResponseMapper.map(ResultExecutor.execute(() -> (projectService.removePackagesFromProject(projectName, packages))), HttpStatus.OK);
    }

    @PutMapping("/{projectName}/users/{collaboratorUsername}")
    @Operation(summary = "Add collaborator", description = "Grants a user access to the project. Only Owner can add collaborators.")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Collaborator added successfully",
            content = @Content(mediaType = "application/json", 
            schema = @Schema(implementation = MessageResponseDTO.class))),
        @ApiResponse(
            responseCode = "404", 
            description = "Project or User not found",
            content = @Content(mediaType = "application/json", 
            schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(
            responseCode = "409", 
            description = "User already collaborator or Owner",
            content = @Content(mediaType = "application/json", 
            schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(
            responseCode = "403", 
            description = "Access Denied",
            content = @Content(mediaType = "application/json", 
            schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<?> addCollaboratorToProject(
            @Parameter(description = "Project name", example = "RiskAnalysis_AI") @PathVariable String projectName, 
            @Parameter(description= "Collaborator username", example = "francesca.romano") @PathVariable String collaboratorUsername
        ) {
        return restResponseMapper.map(ResultExecutor.execute(() -> (projectService.addCollaboratorToProject(projectName, collaboratorUsername))), HttpStatus.OK);
    }

    @PutMapping("/{projectName}/admin/{newAdminUsername}")
    @Operation(summary = "Transfer ownership", description = "Transfers the project admin role to another existing collaborator.")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Ownership transferred successfully",
            content = @Content(mediaType = "application/json", 
            schema = @Schema(implementation = MessageResponseDTO.class))),
        @ApiResponse(
            responseCode = "403", 
            description = "Only the current owner can transfer ownership",
            content = @Content(mediaType = "application/json", 
            schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(
            responseCode = "404", 
            description = "Project or new Admin not found",
            content = @Content(mediaType = "application/json", 
            schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(
            responseCode = "400", 
            description = "The new admin must be a current collaborator",
            content = @Content(mediaType = "application/json", 
            schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<?> transferOwnership(
            @Parameter(description = "Project name", example = "RiskAnalysis_AI") @PathVariable String projectName,
            @Parameter(description = "Username of the new administrator", example = "francesca.romano") @PathVariable String newAdminUsername
    ) {
        return restResponseMapper.map(ResultExecutor.execute(() -> projectService.transferOwnership(projectName, newAdminUsername)), HttpStatus.OK);
    }

    @GetMapping("/{projectName}/users")
    @Operation(summary = "List collaborators", description = "Get the list of all collaborators for the project.")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "List of collaborators retrieved",
            content = @Content(mediaType = "application/json", 
            array = @ArraySchema(schema = @Schema(implementation = CollaboratorDTO.class)))),
        @ApiResponse(
            responseCode = "403", 
            description = "Access Denied",
            content = @Content(mediaType = "application/json", 
            schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(
            responseCode = "404", 
            description = "Project not found",
            content = @Content(mediaType = "application/json", 
            schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<?> getCollaboratorsOfProject(@Parameter(description = "Project name", example = "RiskAnalysis_AI") @PathVariable String projectName) {
        return restResponseMapper.map(ResultExecutor.execute(() -> (projectService.getProjectCollaborators(projectName))), HttpStatus.OK);
    }   
    
    @DeleteMapping("/{projectName}/users/{collaboratorUsername}")
    @Operation(summary = "Remove collaborator", description = "Revokes access to a collaborator. Only Owner can remove collaborators.")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Collaborator removed successfully",
            content = @Content(mediaType = "application/json", 
            schema = @Schema(implementation = MessageResponseDTO.class))),
        @ApiResponse(
            responseCode = "400", 
            description = "Cannot remove Owner",
            content = @Content(mediaType = "application/json", 
            schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(
            responseCode = "404", 
            description = "Project or Collaborator not found",
            content = @Content(mediaType = "application/json", 
            schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(
            responseCode = "403", 
            description = "Access Denied",
            content = @Content(mediaType = "application/json", 
            schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<?> removeCollaboratorFromProject(
            @Parameter(description = "Project name", example = "RiskAnalysis_AI") @PathVariable String projectName,
            @Parameter(description= "Collaborator username", example = "francesca.romano") @PathVariable String collaboratorUsername
    ) {
        return restResponseMapper.map(ResultExecutor.execute(() -> (projectService.removeCollaboratorFromProject(projectName, collaboratorUsername))), HttpStatus.OK);
    }
    
    @DeleteMapping("/{projectName}/users/me")
    @Operation(summary = "Leave project", description = "The authenticated user voluntarily leaves the project.")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Successfully left the project",
            content = @Content(mediaType = "application/json", 
            schema = @Schema(implementation = MessageResponseDTO.class))),
        @ApiResponse(
            responseCode = "400", 
            description = "Owners cannot leave without transferring ownership first",
            content = @Content(mediaType = "application/json", 
            schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(
            responseCode = "404", 
            description = "Project not found",
            content = @Content(mediaType = "application/json", 
            schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<?> leaveProject(
            @Parameter(description = "Project name", example = "RiskAnalysis_AI") @PathVariable String projectName
    ) {
        return restResponseMapper.map(ResultExecutor.execute(() -> projectService.leaveProject(projectName)), HttpStatus.OK);
    }
}
