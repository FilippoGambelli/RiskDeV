package it.unipi.riskDeV.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.unipi.riskDeV.DTO.InstalledPackageDTO;
import it.unipi.riskDeV.DTO.ProjectDTO;
import it.unipi.riskDeV.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

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
public class ProjectController {
    
    private final ProjectService projectService;

    @GetMapping("/")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "List of all the projects in which the user is a collaborator",
            description = "Fetches a list of all the projects in which the user is a collaborator.")
    public List<String> getAllUserProjects() {
        log.info("Searching all the projects in which the user is a collaborator.");
        return projectService.getAllUserProjects();
    }

    @PostMapping("/{projectId}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Insert a new project",
            description = "Insert a new project for the current user.")
    public String insertProject(@Valid @RequestBody ProjectDTO projectDTO) {
        log.info("Inserting a new project for the current user.");
        return projectService.insertProject(projectDTO);
    }

    @DeleteMapping("/{projectId}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete a project",
            description = "Deletes a project for the current user.")
    public String deleteProject(@PathVariable String projectId) {
        log.info("Deleting a project for the current user.");
        return projectService.deleteProject(projectId);
    }

    @PutMapping("/{projectId}/packages")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update project packages",
            description = "Updates the packages of a project for the current user.")
    public String updateProjectPackages(@PathVariable String projectId, @Valid @RequestBody List<InstalledPackageDTO> packages) {
        log.info("Updating packages of a project for the current user.");
        return projectService.updateProjectPackages(projectId, packages);
    }

    @DeleteMapping("/{projectId}/packages")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Remove packages from project",    
            description = "Removes packages from a project for the current user.")
    public String removePackagesFromProject(@PathVariable String projectId, @Valid @RequestBody List<InstalledPackageDTO> packages) {
        log.info("Removing packages from a project for the current user.");
        return projectService.removePackagesFromProject(projectId, packages);
    }

    @PutMapping("/{projectId}/users")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Add collaborator to project",
            description = "Adds a collaborator to a project for the current user.")
    public String addCollaboratorToProject(@PathVariable String projectId, @RequestBody String collaboratorUsername) {
        log.info("Adding a collaborator to a project for the current user.");
        return projectService.addCollaboratorToProject(projectId, collaboratorUsername);
    }

    @GetMapping("/{projectId}/users")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get collaborators of project",
            description = "Fetches the list of collaborators of a project for the current user.")
    public List<String> getCollaboratorsOfProject(@PathVariable String projectId) {
        log.info("Fetching collaborators of a project for the current user.");
        return projectService.getProjectCollaborators(projectId);
    }   
    
    @DeleteMapping("/{projectId}/users")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Remove collaborator from project",
            description = "Removes a collaborator from a project for the current user.")
    public String removeCollaboratorFromProject(@PathVariable String projectId, @RequestBody String collaboratorUsername) {
        log.info("Removing a collaborator from a project for the current user.");
        return projectService.removeCollaboratorFromProject(projectId, collaboratorUsername);
    }
    
}
