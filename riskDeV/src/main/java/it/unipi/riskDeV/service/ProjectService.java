package it.unipi.riskDeV.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.unipi.riskDeV.DTO.InstalledPackageDTO;
import it.unipi.riskDeV.DTO.project.ProjectCreationDTO;
import it.unipi.riskDeV.DTO.project.ProjectDTO;
import it.unipi.riskDeV.common.DomainError;
import it.unipi.riskDeV.common.Result;
import it.unipi.riskDeV.event.ProjectEvents;
import it.unipi.riskDeV.event.ProjectEvents.CalculateRiskMetricsEvent;
import it.unipi.riskDeV.event.ProjectEvents.CollaboratorAddedEvent;
import it.unipi.riskDeV.event.ProjectEvents.CollaboratorRemovedEvent;
import it.unipi.riskDeV.event.ProjectEvents.ProjectDeletedEvent;
import it.unipi.riskDeV.event.ProjectEvents.ProjectPackagesUpdatedEvent;
import it.unipi.riskDeV.repository.ProjectRepository;
import it.unipi.riskDeV.repository.UserRepository;
import it.unipi.riskDeV.model.Project;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    public Result<ProjectDTO> getProject(String projectName) {
        var projectOpt = projectRepository.findByName(projectName);
        if (projectOpt.isEmpty()) {
            return new Result.Failure<>(new DomainError.NotFound("Project not found"));
        }

        Project project = projectOpt.get();
        String username = getCurrentUsername();
        boolean isOwner = project.getAdmin().getUsername().equals(username);
        boolean isCollaborator = project.getCollaborators().stream()
                .anyMatch(c -> c.getUsername().equals(username));

        if (!isOwner && !isCollaborator) {
            return new Result.Failure<>(new DomainError.AccessDenied("You are not authorized to view this project."));
        }

        return new Result.Success<>(new ProjectDTO(project));
    }

    @Transactional
    public Result<ProjectDTO> addProject(ProjectCreationDTO projectCreationDTO) {
        log.info("Starting insert transaction for user: {}", getCurrentUsername());

        try {
            Project project = new Project(projectCreationDTO);
            Project.Collaborator admin = getCollaboratorInfo(getCurrentUsername());
            List<Project.Collaborator> collaborators = new ArrayList<>();
            collaborators.add(admin);

            project.setAdmin(admin);
            project.setCollaborators(collaborators);
            project.setLastUpdate(Instant.now());

            Project savedProject = projectRepository.save(project);
            log.info("Project {} saved successfully in Mongo.", savedProject.getName());

            List<Project.ProjectPackage> packages = savedProject.getPackages();
            
            // Publish event to evaluate risk metrics
            CalculateRiskMetricsEvent calculateRiskMetricsEvent = new CalculateRiskMetricsEvent(
                savedProject.getName(),
                packages.stream()
                    .map(p -> new InstalledPackageDTO(p.getName(), p.getVersion()))
                    .toList()
            );
            eventPublisher.publishEvent(calculateRiskMetricsEvent);

            // Package list for event
            List<String> pkgsForGraph = packages.stream()
                .map(p -> p.getName() + ":" + p.getVersion())
                .toList();

            // Publish project created event to create project node in graph and sincrhonize admin user
            var event = new ProjectEvents.ProjectCreatedEvent(savedProject.getName(), savedProject.getAdmin().getUsername(), pkgsForGraph);
            eventPublisher.publishEvent(event);

            return new Result.Success<>(new ProjectDTO(savedProject));
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
            projectRepository.delete(project);
            log.info("Project {} deleted successfully from Mongo.", projectName);

            // Publish project deleted event to remove project node and sinchronize collaborators
            eventPublisher.publishEvent(new ProjectDeletedEvent(
                projectName, 
                project.getCollaborators().stream()
                    .map(Project.Collaborator::getUsername)
                    .toList()
            ));
            
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

                //TODO: Consider versioning strategy (multiple versions of same package?)
                //project.getPackages().removeIf(p -> p.getName().equals(dto.getName()));

                // Initialization of score and vuln count to 0, will be updated after fetching metrics
                Project.ProjectPackage pkg = new Project.ProjectPackage();
                pkg.setName(dto.getName());
                pkg.setVersion(dto.getVersion());
                pkg.setRiskScore(0.0);
                pkg.setVulnerabilitiesCount(0);

                project.getPackages().add(pkg);
            }

            project.setLastUpdate(Instant.now());
            Project savedProject = projectRepository.save(project);
            log.info("Project {} packages updated successfully in Mongo.", projectName);

            List<InstalledPackageDTO> packages = savedProject.getPackages().stream()
                .map(p -> new InstalledPackageDTO(p.getName(), p.getVersion()))
                .toList();
            List<String> allPackageIds = project.getPackages().stream()
                .map(p -> p.getName() + " " + p.getVersion())
                .toList();
            eventPublisher.publishEvent(new ProjectPackagesUpdatedEvent(projectName, allPackageIds));
            eventPublisher.publishEvent(new CalculateRiskMetricsEvent(projectName, packages));

            return new Result.Success<>(projectName);

        } catch (Exception e) {
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
             return new Result.Failure<>(new DomainError.InvalidOperation("User is already the owner"));
        }

        try {
            var newCollabInfo = getCollaboratorInfo(collaboratorUsername);
            if (project.getCollaborators().stream().anyMatch(c -> c.getUsername().equals(collaboratorUsername))) {
                return new Result.Failure<>(new DomainError.InvalidOperation("User already is a collaborator"));
            }

            project.getCollaborators().add(newCollabInfo);
            project.setLastUpdate(Instant.now());

            projectRepository.save(project);
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
            eventPublisher.publishEvent(new CollaboratorRemovedEvent(projectName, collaboratorUsername));

            log.info("Removed collaborator {} from project {} on Mongo.", collaboratorUsername, projectName);
            return new Result.Success<>(username);

        } catch (Exception e) {
            log.error("Error removing collaborator {} from project {}", collaboratorUsername, projectName, e);
            return new Result.Failure<>(new DomainError.SystemError("Error removing collaborator.", e));
        }
    }

    /* Utility methods */
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

    private Project.Collaborator getCollaboratorInfo(String username) {
        return userRepository.findByUsername(username)
            .map(user -> {
                Project.Collaborator collaborator = new Project.Collaborator();
                collaborator.setUsername(user.getUsername());
                collaborator.setEmail(user.getEmail());
                return collaborator;
            })
            .orElseThrow(() -> new IllegalStateException("User " + username + " not found."));
    }

}
