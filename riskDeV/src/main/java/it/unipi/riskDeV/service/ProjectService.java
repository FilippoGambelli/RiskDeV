package it.unipi.riskDeV.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import it.unipi.riskDeV.DTO.InstalledPackageDTO;
import it.unipi.riskDeV.DTO.ProjectDTO;
import it.unipi.riskDeV.common.DomainError;
import it.unipi.riskDeV.common.Result;
import it.unipi.riskDeV.event.ProjectEvents;
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

    public Result<Void> insertProject(String userId, ProjectDTO projectDTO) {
        log.info("Starting insert transaction for user: {}", userId);

        if (projectRepository.existsById(projectDTO.getName())) {
            return new Result.Failure<>(new DomainError.AlreadyExists("Project with name " + projectDTO.getName() + " already exists."));
        }

        Project project = new Project();
        project.setName(projectDTO.getName());
        project.setDescription(projectDTO.getDescription());
        project.setPythonVersion(projectDTO.getPythonVersion());
        project.setAdminId(userId);
        project.setLastUpdate(LocalDateTime.now());

        List<String> pkgsForGraph = new ArrayList<>();
        if (projectDTO.getPackages() != null) {
            for (InstalledPackageDTO pkgDTO : projectDTO.getPackages()) {
                Project.ProjectPackage pkg = new Project.ProjectPackage();
                pkg.setName(pkgDTO.getName());
                pkg.setVersion(pkgDTO.getVersion());
                pkg.setRiskScore(0.0);
                pkg.setVulnerabilitiesCount(0);

                project.getPackages().add(pkg);
                pkgsForGraph.add(pkgDTO.getName() + " " + pkgDTO.getVersion());
            }
        }

        try {
            projectRepository.save(project);
            log.info("Project {} saved successfully in Mongo.", projectDTO.getName());

            var event = new ProjectEvents.ProjectCreatedEvent(
                projectDTO.getName(), projectDTO.getAdminId(), pkgsForGraph
            );
            eventPublisher.publishEvent(event);

            return new Result.Success<>();
        } catch (Exception e) {
            log.error("Error creating project", e);
            return new Result.Failure<>(new DomainError.SystemError("Creation failed", e));
        } 

    }

    public Result<String> deleteProject(String userId, String projectName) {
        var projectOpt = projectRepository.findById(projectName);
        if (projectOpt.isEmpty()) 
            return new Result.Failure<>(new DomainError.NotFound("Project not found"));

        Project project = projectOpt.get();
        if (!project.getAdminId().equals(userId)) {
            return new Result.Failure<>(new DomainError.AccessDenied("Only the owner can delete the project"));
        }

        try {
            projectRepository.delete(project);
            log.info("Project {} deleted successfully from Mongo.", projectName);
            eventPublisher.publishEvent(new ProjectDeletedEvent(projectName));
            
            return new Result.Success<>(projectName);
        } catch (Exception e) {
            return new Result.Failure<>(new DomainError.SystemError("Delete failed", e));
        }
    }

    public Result<String> updateProjectPackages(String userId, String projectId, List<InstalledPackageDTO> newPackages) {

        var projectOpt = projectRepository.findById(projectId);
        if (projectOpt.isEmpty()) {
            return new Result.Failure<>(new DomainError.NotFound("Project not found"));
        }

        if (newPackages == null || newPackages.isEmpty()) {
            return new Result.Failure<>(new DomainError.ValidationFailed("Package list cannot be null or empty."));
        }

        var project = projectOpt.get();
        if (!canEdit(userId, project)) {
            return new Result.Failure<>(new DomainError.AccessDenied("Permission denied"));
        }
        
        log.info("Adding packages to project {} for user {}", projectId, userId);
        try {
            for (InstalledPackageDTO dto : newPackages) {
                project.getPackages().removeIf(p -> p.getName().equals(dto.getName()));

                var pkg = new Project.ProjectPackage();
                pkg.setName(dto.getName());
                pkg.setVersion(dto.getVersion());
                pkg.setRiskScore(0.0);
                pkg.setVulnerabilitiesCount(0);
                project.getPackages().add(pkg);
            }
            project.setLastUpdate(LocalDateTime.now());
            projectRepository.save(project);
            log.info("Project {} packages updated successfully in Mongo.", projectId);

            List<String> allPackageIds = project.getPackages().stream()
                .map(p -> p.getName() + " " + p.getVersion())
                .toList();
            eventPublisher.publishEvent(new ProjectPackagesUpdatedEvent(projectId, allPackageIds));

            return new Result.Success<>(projectId);

        } catch (DataAccessException e) {
            log.error("Error updating packages for project {}", projectId, e);
            return new Result.Failure<>(new DomainError.SystemError("Failed to update project packages.", e));
        }
    }

    public Result<Void> removePackagesFromProject(String userId, String projectId, List<InstalledPackageDTO> packages) {
        log.info("Removing packages {} from project {}", packages, projectId);

        var projectOpt = projectRepository.findById(projectId);
        if (projectOpt.isEmpty()) {
            return new Result.Failure<>(new DomainError.NotFound("Project not found"));
        }

        Project project = projectOpt.get();
        if (!canEdit(userId, project)) {
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
                return new Result.Success<>();
            }

            project.setLastUpdate(LocalDateTime.now());
            projectRepository.save(project);
            log.info("Project {} updated on Mongo. Packages removed.", projectId);

            List<String> remainingPackageIds = project.getPackages().stream()
                .map(p -> p.getName() + " " + p.getVersion())
                .toList();

            eventPublisher.publishEvent(new ProjectPackagesUpdatedEvent(projectId, remainingPackageIds));

            return new Result.Success<>();

        } catch (Exception e) {
            log.error("Failed to remove packages from project {}", projectId, e);
            return new Result.Failure<>(new DomainError.SystemError("Failed to update project packages.", e));
        }
    }

    public Result<Void> addCollaboratorToProject(String userId, String projectId, String collaboratorUserId) {
        log.info("Request to add collaborator {} to project {} by user {}", collaboratorUserId, projectId, userId);

        var projectOpt = projectRepository.findById(projectId);
        if (projectOpt.isEmpty()) return new Result.Failure<>(new DomainError.NotFound("Project not found"));
        Project project = projectOpt.get();

        if (!project.getAdminId().equals(userId)) {
            return new Result.Failure<>(new DomainError.AccessDenied("Only owner can add collaborators"));
        }

        if (project.getAdminId().equals(collaboratorUserId)) {
             return new Result.Failure<>(new DomainError.InvalidOperation("User is already owner"));
        }

        var collabUserOpt = userRepository.findById(collaboratorUserId);
        if (collabUserOpt.isEmpty()) {
            return new Result.Failure<>(new DomainError.NotFound("User not found"));
        }

        if (project.getCollaborators().stream().anyMatch(c -> c.getId().equals(collaboratorUserId))) {
             return new Result.Failure<>(new DomainError.InvalidOperation("User already is a collaborator"));
        }

        try {
            var newCollaborator = collabUserOpt.get();
            var newCollab = new Project.Collaborator();
            newCollab.setId(newCollaborator.getId());
            newCollab.setUsername(newCollaborator.getUsername());
            newCollab.setEmail(newCollaborator.getEmail());

            project.getCollaborators().add(newCollab);
            projectRepository.save(project);
            eventPublisher.publishEvent(new CollaboratorAddedEvent(projectId, collaboratorUserId));

            return new Result.Success<>(null);
        } catch (Exception e) {
            return new Result.Failure<>(new DomainError.SystemError("Failed to add collaborator", e));
        }
    }

    public Result<List<Project.Collaborator>> getProjectCollaborators(String userId, String projectId) {

        var projectOpt = projectRepository.findById(projectId);
        if (projectOpt.isEmpty()) {
            return new Result.Failure<>(new DomainError.NotFound("Project with ID " + projectId + " not found."));
        }

        var project = projectOpt.get();
        boolean isOwner = project.getAdminId().equals(userId);
        boolean isCollaborator = project.getCollaborators().stream()
                .anyMatch(c -> c.getId().equals(userId));

        if (!isOwner && !isCollaborator) {
            return new Result.Failure<>(new DomainError.AccessDenied("You are not authorized to view this project's collaborators."));
        }

        return new Result.Success<>(project.getCollaborators());
    }

    public Result<String> removeCollaboratorFromProject(String userId, String projectId, String collaboratorUserId) {
        log.info("Removing collaborator {} from project {} requested by {}", collaboratorUserId, projectId, userId);

        var projectOpt = projectRepository.findById(projectId);
        if (projectOpt.isEmpty()) {
            return new Result.Failure<>(new DomainError.NotFound("Project not found"));
        }
        
        var project = projectOpt.get();
        boolean isRequesterOwner = project.getAdminId().equals(userId);
        boolean isSelfRemoval = userId.equals(collaboratorUserId); 

        if (!isRequesterOwner) {
            return new Result.Failure<>(new DomainError.AccessDenied("Only the project owner can remove collaborators."));
        }

        if (isSelfRemoval) {
            return new Result.Failure<>(new DomainError.InvalidOperation("The owner cannot be removed from the project."));
        }

        try {
            boolean removed = project.getCollaborators().removeIf(c -> c.getId().equals(collaboratorUserId));
            if (!removed) {
                return new Result.Failure<>(new DomainError.NotFound("Collaborator not found in this project."));
            }

            project.setLastUpdate(LocalDateTime.now());
            projectRepository.save(project);
            eventPublisher.publishEvent(new CollaboratorRemovedEvent(projectId, collaboratorUserId));

            log.info("Removed collaborator {} from project {} on Mongo.", collaboratorUserId, projectId);
            return new Result.Success<>(userId);

        } catch (Exception e) {
            log.error("Error removing collaborator {} from project {}", collaboratorUserId, projectId, e);
            return new Result.Failure<>(new DomainError.SystemError("Error removing collaborator.", e));
        }
    }

    // Utility method
    private boolean canEdit(String userId, Project project) {
        return project.getAdminId().equals(userId) || 
               project.getCollaborators().stream().anyMatch(c -> c.getId().equals(userId));
    }

}
