package it.unipi.riskDeV.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.unipi.riskDeV.DTO.InstalledPackageDTO;
import it.unipi.riskDeV.DTO.PackageRiskMetricsDTO;
import it.unipi.riskDeV.DTO.PackageVersionDTO;
import it.unipi.riskDeV.DTO.UserDTO;
import it.unipi.riskDeV.DTO.project.ProjectCreationDTO;
import it.unipi.riskDeV.DTO.project.ProjectDTO;
import it.unipi.riskDeV.common.DomainError;
import it.unipi.riskDeV.common.Result;
import it.unipi.riskDeV.event.ProjectEvents;
import it.unipi.riskDeV.event.ProjectEvents.CollaboratorAddedEvent;
import it.unipi.riskDeV.event.ProjectEvents.CollaboratorRemovedEvent;
import it.unipi.riskDeV.event.ProjectEvents.ProjectDeletedEvent;
import it.unipi.riskDeV.event.ProjectEvents.ProjectPackagesUpdatedEvent;
import it.unipi.riskDeV.mapper.ProjectMapper;
import it.unipi.riskDeV.repository.ProjectRepository;
import it.unipi.riskDeV.model.Project;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMapper projectMapper;
    //TODO: Event Driven Pattern
    private final UserService userService;
    //TODO: Event Driven Pattern
    private final PackageService packageService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Result<ProjectDTO> insertProject(ProjectCreationDTO projectCreationDTO) {
        log.info("Starting insert transaction for user: {}", getCurrentUsername());

        try {
            Project project = projectMapper.toEntity(projectCreationDTO);
            Project.Collaborator admin = getAuthenticatedCollaborator();
            List<Project.Collaborator> collaborators = new ArrayList<>();
            collaborators.add(admin);

            project.setAdmin(admin);
            project.setCollaborators(collaborators);
            project.setLastUpdate(Instant.now());

            if (project.getPackages() != null) {
                project.getPackages().forEach(pkg -> {
                    var metrics = fetchPackageRiskMetrics(pkg.getName(), pkg.getVersion());
                    pkg.setRiskScore(metrics.riskScore());
                    pkg.setVulnerabilitiesCount(metrics.vulnerabilitiesCount());
                });
            }

            Project savedProject = projectRepository.save(project);
            // Add project to admin's project list, use UserService method to manage concurrency
            userService.addProjectToUser(savedProject.getAdmin().getUsername(), savedProject.getName());
            log.info("Project {} saved successfully in Mongo.", savedProject.getName());

            List<String> pkgsForGraph = (project.getPackages() != null) ? project.getPackages().stream()
                .map(p -> p.getName() + ":" + p.getVersion())
                .toList() : List.of();

            var event = new ProjectEvents.ProjectCreatedEvent(savedProject.getName(), savedProject.getAdmin().getUsername(), pkgsForGraph);
            eventPublisher.publishEvent(event);

            return new Result.Success<>(projectMapper.toDto(savedProject));
        } catch (IllegalArgumentException e) {
            log.error("Validation error during project creation", e);
            return new Result.Failure<>(new DomainError.ValidationFailed("Invalid project data: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Error creating project", e);
            return new Result.Failure<>(new DomainError.SystemError("Creation failed", e));
        } 

    }

    @Transactional
    public Result<String> deleteProject(String projectName) {
        var projectOpt = projectRepository.findByName(projectName);
        if (projectOpt.isEmpty()) 
            return new Result.Failure<>(new DomainError.NotFound("Project not found"));

        Project project = projectOpt.get();
        String username = getCurrentUsername();
        if (!project.getAdmin().getUsername().equals(username)) {
            return new Result.Failure<>(new DomainError.AccessDenied("Only the owner can delete the project"));
        }

        try {
            //Remove project from users' project lists, use UserService method to manage concurrency
            for (Project.Collaborator collaborator : project.getCollaborators()) {
                userService.removeProjectFromUser(collaborator.getUsername(), projectName);
            }

            projectRepository.delete(project);
            log.info("Project {} deleted successfully from Mongo.", projectName);
            eventPublisher.publishEvent(new ProjectDeletedEvent(projectName));
            
            return new Result.Success<>(projectName);
        } catch (Exception e) {
            return new Result.Failure<>(new DomainError.SystemError("Delete failed", e));
        }
    }

    public Result<String> updateProjectPackages(String projectName, List<InstalledPackageDTO> newPackages) {

        var projectOpt = projectRepository.findByName(projectName);
        if (projectOpt.isEmpty()) {
            return new Result.Failure<>(new DomainError.NotFound("Project not found"));
        }

        if (newPackages == null || newPackages.isEmpty()) {
            return new Result.Failure<>(new DomainError.ValidationFailed("Package list cannot be null or empty."));
        }

        var project = projectOpt.get();
        String username = getCurrentUsername();
        if (!canEdit(username, project)) {
            return new Result.Failure<>(new DomainError.AccessDenied("Permission denied"));
        }
        
        log.info("Adding packages to project {} for user {}", projectName, username);
        try {

            for (InstalledPackageDTO dto : newPackages) {
                
                if (project.getPackages() == null) {
                    project.setPackages(new ArrayList<>());
                }

                project.getPackages().removeIf(p -> p.getName().equals(dto.getName()));

                Project.ProjectPackage pkg = new Project.ProjectPackage();
                pkg.setName(dto.getName());
                pkg.setVersion(dto.getVersion());

                var metrics = fetchPackageRiskMetrics(dto.getName(), dto.getVersion());
                pkg.setRiskScore(metrics.riskScore());
                pkg.setVulnerabilitiesCount(metrics.vulnerabilitiesCount());

                project.getPackages().add(pkg);
            }

            project.setLastUpdate(Instant.now());
            projectRepository.save(project);
            log.info("Project {} packages updated successfully in Mongo.", projectName);

            List<String> allPackageIds = project.getPackages().stream()
                .map(p -> p.getName() + " " + p.getVersion())
                .toList();
            eventPublisher.publishEvent(new ProjectPackagesUpdatedEvent(projectName, allPackageIds));

            return new Result.Success<>(projectName);

        } catch (DataAccessException e) {
            log.error("Error updating packages for project {}", projectName, e);
            return new Result.Failure<>(new DomainError.SystemError("Failed to update project packages.", e));
        }
    }

    public Result<String> removePackagesFromProject(String projectName, List<InstalledPackageDTO> packages) {
        log.info("Removing packages {} from project {}", packages, projectName);

        var projectOpt = projectRepository.findByName(projectName);
        if (projectOpt.isEmpty()) {
            return new Result.Failure<>(new DomainError.NotFound("Project not found"));
        }

        Project project = projectOpt.get();
        if (!canEdit(getCurrentUsername(), project)) {
            return new Result.Failure<>(new DomainError.AccessDenied("Permission denied"));
        }

        try {
            boolean changed = false;

            for (InstalledPackageDTO dtoToRemove : packages) {
                boolean removed = project.getPackages().removeIf(p -> 
                    p.getName().equals(dtoToRemove.getName()) && 
                    p.getVersion().equals(dtoToRemove.getVersion())
                );
                if (removed) 
                    changed = true;
            }

            if (!changed) {
                return new Result.Success<>(null);
            }

            project.setLastUpdate(Instant.now());
            projectRepository.save(project);
            log.info("Project {} updated on Mongo. Packages removed.", projectName);

            List<String> remainingPackageIds = project.getPackages().stream()
                .map(p -> p.getName() + " " + p.getVersion())
                .toList();

            eventPublisher.publishEvent(new ProjectPackagesUpdatedEvent(projectName, remainingPackageIds));

            return new Result.Success<>("Packages removed successfully.");

        } catch (Exception e) {
            log.error("Failed to remove packages from project {}", projectName, e);
            return new Result.Failure<>(new DomainError.SystemError("Failed to update project packages.", e));
        }
    }

    @Transactional
    public Result<String> addCollaboratorToProject(String projectName, String collaboratorUsername) {
        log.info("Request to add collaborator {} to project {}", collaboratorUsername, projectName);

        var projectOpt = projectRepository.findByName(projectName);
        if (projectOpt.isEmpty()) return new Result.Failure<>(new DomainError.NotFound("Project not found"));
        
        String username = getCurrentUsername();
        Project project = projectOpt.get();
        if (!project.getAdmin().getUsername().equals(username)) {
            return new Result.Failure<>(new DomainError.AccessDenied("Only owner can add collaborators"));
        }

        if (project.getAdmin().getUsername().equals(collaboratorUsername)) {
             return new Result.Failure<>(new DomainError.InvalidOperation("User is already owner"));
        }

        Result<UserDTO> userResult = userService.getProfileByUsername(collaboratorUsername);
        if (userResult instanceof Result.Failure<UserDTO> failure) {
            return new Result.Failure<>(failure.error());
        }
        UserDTO user = ((Result.Success<UserDTO>) userResult).data();

        if (project.getCollaborators().stream().anyMatch(c -> c.getUsername().equals(collaboratorUsername))) {
             return new Result.Failure<>(new DomainError.InvalidOperation("User already is a collaborator"));
        }

        try {
            var newCollab = new Project.Collaborator();
            newCollab.setUsername(user.username());
            newCollab.setEmail(user.email());
            project.getCollaborators().add(newCollab);
            project.setLastUpdate(Instant.now());

            projectRepository.save(project);
            // Add project to collaborator's project list, use UserService method to manage concurrency
            userService.addProjectToUser(collaboratorUsername, projectName);
            log.info("Added collaborator {} to project {} on Mongo.", collaboratorUsername, projectName);
            eventPublisher.publishEvent(new CollaboratorAddedEvent(projectName, collaboratorUsername));

            return new Result.Success<>("Added collaborator " + collaboratorUsername + " to project " + projectName);
        } catch (Exception e) {
            return new Result.Failure<>(new DomainError.SystemError("Failed to add collaborator", e));
        }
    }

    public Result<List<Project.Collaborator>> getProjectCollaborators(String projectName) {

        var projectOpt = projectRepository.findByName(projectName);
        if (projectOpt.isEmpty()) {
            return new Result.Failure<>(new DomainError.NotFound("Project with name " + projectName + " not found."));
        }

        String username = getCurrentUsername();
        var project = projectOpt.get();
        boolean isOwner = project.getAdmin().getUsername().equals(username);
        boolean isCollaborator = project.getCollaborators().stream()
                .anyMatch(c -> c.getUsername().equals(username));

        if (!isOwner && !isCollaborator) {
            return new Result.Failure<>(new DomainError.AccessDenied("You are not authorized to view this project's collaborators."));
        }

        return new Result.Success<>(project.getCollaborators());
    }

    @Transactional
    public Result<String> removeCollaboratorFromProject(String projectName, String collaboratorUsername) {
        log.info("Removing collaborator {} from project {}", collaboratorUsername, projectName);

    
        var projectOpt = projectRepository.findByName(projectName);
        if (projectOpt.isEmpty()) {
            return new Result.Failure<>(new DomainError.NotFound("Project not found"));
        }

        String username = getCurrentUsername();
        var project = projectOpt.get();
        boolean isRequesterOwner = project.getAdmin().getUsername().equals(username);
        boolean isSelfRemoval = username.equals(collaboratorUsername); 

        if (!isRequesterOwner) {
            return new Result.Failure<>(new DomainError.AccessDenied("Only the project owner can remove collaborators."));
        }

        if (isSelfRemoval) {
            return new Result.Failure<>(new DomainError.InvalidOperation("The owner cannot be removed from the project."));
        }

        try {
            boolean removed = project.getCollaborators().removeIf(c -> c.getUsername().equals(collaboratorUsername));
            if (!removed) {
                return new Result.Failure<>(new DomainError.NotFound("Collaborator not found in this project."));
            }

            project.setLastUpdate(Instant.now());
            projectRepository.save(project);
            // Remove project from collaborator's project list, use UserService method to manage concurrency
            userService.removeProjectFromUser(collaboratorUsername, projectName);
            eventPublisher.publishEvent(new CollaboratorRemovedEvent(projectName, collaboratorUsername));

            log.info("Removed collaborator {} from project {} on Mongo.", collaboratorUsername, projectName);
            return new Result.Success<>(username);

        } catch (Exception e) {
            log.error("Error removing collaborator {} from project {}", collaboratorUsername, projectName, e);
            return new Result.Failure<>(new DomainError.SystemError("Error removing collaborator.", e));
        }
    }

    // Utility methods
    private boolean canEdit(String username, Project project) {
        return project.getAdmin().getUsername().equals(username) || 
               project.getCollaborators().stream().anyMatch(c -> c.getUsername().equals(username));
    }

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("User is not authenticated");
        }
        return auth.getName();
    }

    private Project.Collaborator getAuthenticatedCollaborator() {
        String username = getCurrentUsername();

        Result<UserDTO> userResult = userService.getProfileByUsername(username);
        if (userResult instanceof Result.Success<UserDTO> success) {
            UserDTO user = success.data();
            Project.Collaborator collaborator = new Project.Collaborator();
            collaborator.setUsername(user.username());
            collaborator.setEmail(user.email());
            return collaborator;
        }

        throw new IllegalStateException("Authenticated user not found in DB");
    }

    private PackageRiskMetricsDTO fetchPackageRiskMetrics(String packageName, String packageVersion) {
        Result<PackageVersionDTO> pkgVersion = packageService.getPackageByNameVersion(packageName, packageVersion);

        if (pkgVersion instanceof Result.Success<PackageVersionDTO> success) {

            PackageVersionDTO dto = success.data(); 

            //TODO: Could we have null risk score or vulnerabilities list?
            Double riskScore = (dto.getRiskScore() != null) ? dto.getRiskScore() : 0.0;
            Integer vulnCount = (dto.getVulnerabilities() != null) ? dto.getVulnerabilities().size() : 0;

            return new PackageRiskMetricsDTO(riskScore, vulnCount);
        }

        return new PackageRiskMetricsDTO(0.0, 0);
    }

}
