package it.unipi.riskDeV.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.unipi.riskDeV.DTO.packageVersion.PackageVersionDTO;
import it.unipi.riskDeV.DTO.project.CollaboratorDTO;
import it.unipi.riskDeV.DTO.project.InstalledPackageDTO;
import it.unipi.riskDeV.DTO.project.ProjectCreationDTO;
import it.unipi.riskDeV.DTO.project.ProjectDTO;
import it.unipi.riskDeV.async.events.ProjectEvent;
import it.unipi.riskDeV.model.documentDB.Project;
import it.unipi.riskDeV.repository.documentDB.ProjectRepository;
import it.unipi.riskDeV.repository.documentDB.UserRepository;
import it.unipi.riskDeV.results.DomainError;
import it.unipi.riskDeV.results.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final PackageService packageService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public Result<ProjectDTO> getProject(String projectName) {
        return projectRepository.findByName(projectName)
            .map(project -> {
                if (!canView(getCurrentUsername(), project)) {
                    return new Result.Failure<ProjectDTO>(new DomainError.AccessDenied("You are not authorized to view this project."));
                }
                return new Result.Success<>(ProjectDTO.fromEntity(project));
            })
            .orElseGet(() -> new Result.Failure<>(new DomainError.NotFound("Project not Found")));
    }

    @Transactional
    public Result<ProjectDTO> addProject(ProjectCreationDTO dto) {
        String username = getCurrentUsername();
        log.info("Starting insert transaction for user: {}", username);

        try {

            Project.Collaborator admin = getCollaboratorInfo(username);
            // Entity build using builder pattern
            Project project  = Project.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .pythonVersion(dto.getPythonVersion())
                .admin(admin)
                .collaborators(List.of(admin))
                .lastUpdate(Instant.now())
                .packages(
                    dto.getPackages().stream()
                        .map(pkgDto -> Project.ProjectPackage.builder()
                            .name(pkgDto.getName())
                            .version(pkgDto.getVersion())
                            .riskScore(0.0)
                            .vulnerabilitiesCount(0)
                            .build())
                        .toList())
                .build();

            Project savedProject = projectRepository.save(project);
            log.info("Project {} saved successfully.", savedProject.getName());

            triggerPostCreationEvents(savedProject);

            return new Result.Success<>(ProjectDTO.fromEntity(savedProject));
        } catch (Exception e) {
            log.error("Error creating project", e);
            return new Result.Failure<>(new DomainError.SystemError());
        } 

    }

    @Transactional
    public Result<String> deleteProject(String projectName) {
        return projectRepository.findByName(projectName)
            .map(project -> {
                if (!project.getAdmin().getUsername().equals(getCurrentUsername())) {
                    return new Result.Failure<String>(new DomainError.AccessDenied("Only the owner can delete the project"));
                }
                
                try {
                    projectRepository.delete(project);
                    
                    List<String> userIds = project.getCollaborators().stream()
                        .map(Project.Collaborator::getUsername)
                        .toList();
                    eventPublisher.publishEvent(new ProjectEvent.ProjectDeleted(projectName, userIds));
                    
                    return new Result.Success<>(projectName);
                } catch (Exception e) {
                    return new Result.Failure<String>(new DomainError.SystemError());
                }
            })
            .orElse(new Result.Failure<>(new DomainError.NotFound("Project not found")));
    }

    public Result<String> updateProjectPackages(String projectName, List<InstalledPackageDTO> newPackages) {
        if (newPackages == null || newPackages.isEmpty()) {
            return new Result.Failure<>(new DomainError.ValidationFailed("Package list cannot be null or empty."));
        }

        var projectOpt = projectRepository.findByName(projectName);
        if (projectOpt.isEmpty()) return new Result.Failure<>(new DomainError.NotFound("Project not found"));
        Project project = projectOpt.get();

        if (!canEdit(getCurrentUsername(), project)) {
            return new Result.Failure<>(new DomainError.AccessDenied("Permission denied"));
        }

        try {
            // TODO: Could be very useful transform this logic in an API call to give missing packages
            List<String> missingPackages = new ArrayList<>();
            for (InstalledPackageDTO pkg : newPackages) {
                if (packageService.getPackageByNameVersion(pkg.getName(), pkg.getVersion()) instanceof Result.Failure) {
                    missingPackages.add(pkg.getName() + "@" + pkg.getVersion());
                }
            }

            if (!missingPackages.isEmpty()) {
                return new Result.Failure<>(new DomainError.ValidationFailed("The following packtes are not in the system: " + String.join(", ", missingPackages)));
            }

            Set<String> newPackageNames = newPackages.stream()
                .map(InstalledPackageDTO::getName)
                .collect(Collectors.toSet());
            
            project.getPackages().removeIf(p -> newPackageNames.contains(p.getName()));

            List<Project.ProjectPackage> pkgsToAdd = newPackages.stream()
                .map(dto -> Project.ProjectPackage.builder()
                        .name(dto.getName())
                        .version(dto.getVersion())
                        .riskScore(0.0)
                        .vulnerabilitiesCount(0)
                        .build())
                .toList();
            
            project.getPackages().addAll(pkgsToAdd);
            
            project.setLastUpdate(Instant.now()); 
            Project savedProject = projectRepository.save(project);

            triggerPostPackageUpdateEvents(savedProject);

            return new Result.Success<>(projectName);

        } catch (Exception e) {
            log.error("Error updating packages for project {}", projectName, e);
            return new Result.Failure<>(new DomainError.SystemError());
        }
    }

    public Result<String> removePackagesFromProject(String projectName, List<String> packagesToRemove) {
        var projectOpt = projectRepository.findByName(projectName);
        if (projectOpt.isEmpty()) return new Result.Failure<>(new DomainError.NotFound("Project not found"));
        Project project = projectOpt.get();

        if (!canEdit(getCurrentUsername(), project)) {
            return new Result.Failure<>(new DomainError.AccessDenied("Permission denied"));
        }

        try {
            boolean changed = project.getPackages().removeIf(p -> 
                packagesToRemove.contains(p.getName())
            );

            if (!changed) return new Result.Success<>("No packages matched for removal.");

            project.setLastUpdate(Instant.now());
            projectRepository.save(project);

            triggerPostPackageUpdateEvents(project);

            return new Result.Success<>("Packages removed successfully.");
        } catch (Exception e) {
            return new Result.Failure<>(new DomainError.SystemError());
        }
    }

    @Transactional
    public Result<String> addCollaboratorToProject(String projectName, String collaboratorUsername) {
        var projectOpt = projectRepository.findByName(projectName);
        if (projectOpt.isEmpty()) return new Result.Failure<>(new DomainError.NotFound("Project not found"));
        Project project = projectOpt.get();

        if (!project.getAdmin().getUsername().equals(getCurrentUsername())) {
            return new Result.Failure<>(new DomainError.AccessDenied("Only owner can add collaborators"));
        }

        try {
            if (project.getAdmin().getUsername().equals(collaboratorUsername) || 
                project.getCollaborators().stream().anyMatch(c -> c.getUsername().equals(collaboratorUsername))) {
                return new Result.Failure<>(new DomainError.InvalidOperation("User is already associated with this project"));
            }

            Project.Collaborator newCollab = getCollaboratorInfo(collaboratorUsername);
            project.getCollaborators().add(newCollab);
            project.setLastUpdate(Instant.now());
            
            projectRepository.save(project);
            
            eventPublisher.publishEvent(new ProjectEvent.CollaboratorAdded(projectName, collaboratorUsername));
            return new Result.Success<>("Collaborator added");

        } catch (IllegalStateException e) {
             return new Result.Failure<>(new DomainError.NotFound(e.getMessage()));
        } catch (Exception e) {
            return new Result.Failure<>(new DomainError.SystemError());
        }
    }

    public Result<List<CollaboratorDTO>> getProjectCollaborators(String projectName) {

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

        List<CollaboratorDTO> dtos = project.getCollaborators().stream()
                .map(CollaboratorDTO::fromEntity)
                .toList(); 

        return new Result.Success<>(dtos);
    }

    public Result<Void> changeCollaboratorDataInProjects(List<String> projectNames, String collaboratorUsername, String newUsername, String newEmail) {
        List<DomainError> errors = new ArrayList<>();

        for (String projectName : projectNames) {
            Result<Void> result = changeCollaboratorData(projectName, collaboratorUsername, newUsername, newEmail);

            if (result instanceof Result.Failure<Void> failure) {
                errors.add(failure.error());
            }
        }

        if (!errors.isEmpty()) {
            log.error("Aggiornamento parziale per {} fallito in alcuni progetti. Errori: {}", collaboratorUsername, errors);
            return new Result.Failure<>(new DomainError.SystemError());
        }

        return new Result.Success<>(null);
    }

    @Transactional
    public Result<String> removeCollaboratorFromProject(String projectName, String collaboratorUsername) {
        var projectOpt = projectRepository.findByName(projectName);
        if (projectOpt.isEmpty()) return new Result.Failure<>(new DomainError.NotFound("Project not found"));
        Project project = projectOpt.get();

        String currentOwner = project.getAdmin().getUsername();
        String requester = getCurrentUsername();

        boolean isOwnerRemoving = currentOwner.equals(requester);
        boolean isSelfRemoving = requester.equals(collaboratorUsername);

        if (!isOwnerRemoving && !isSelfRemoving) {
            return new Result.Failure<>(new DomainError.AccessDenied("You don't have permission to remove this collaborator"));
        }

        if (currentOwner.equals(collaboratorUsername)) {
            return new Result.Failure<>(new DomainError.InvalidOperation("Owner cannot be removed. Transfer ownership or delete project."));
        }

        boolean removed = project.getCollaborators().removeIf(c -> c.getUsername().equals(collaboratorUsername));
        
        if (removed) {
            project.setLastUpdate(Instant.now());
            projectRepository.save(project);
            eventPublisher.publishEvent(new ProjectEvent.CollaboratorRemoved(projectName, collaboratorUsername));
            return new Result.Success<>(collaboratorUsername);
        } else {
            return new Result.Failure<>(new DomainError.NotFound("Collaborator not found"));
        }
    }

    public Result<Void> removeCollaboratorFromProjects(List<String> projectsName, String username) {
        List<DomainError> errors = new ArrayList<>();

        for (String pName : projectsName) {
            Result<String> result = removeCollaboratorFromProject(pName, username);

            if (result instanceof Result.Failure<?> failure) {
                errors.add(failure.error());
            }
        }

        if (!errors.isEmpty()) {
            log.error("Failed removal of user {} from a project.", username);
            return new Result.Failure<>(new DomainError.SystemError());
        }

        return new Result.Success<>(null);
    }

    public Result<Void> updateRiskMetrics(String projectName) {
        var projectOpt = projectRepository.findByName(projectName);
        if (projectOpt.isEmpty()) {
            log.warn("Project {} not found during risk calculation. Skipping.", projectName);
            return new Result.Failure<>(new DomainError.NotFound("Collaborator not found"));
        }

        Project project = projectOpt.get();
        boolean dataChanged = false;

        if (project.getPackages() != null) {
            for (Project.ProjectPackage pkg : project.getPackages()) {
                try {
                    // Use of package service to get latest risk data
                    Result<PackageVersionDTO> result = packageService.getPackageByNameVersion(pkg.getName(), pkg.getVersion());

                    if (result instanceof Result.Success<PackageVersionDTO> success) {
                        PackageVersionDTO details = success.data();
                        
                        double newScore = (details.getRiskScore() != null) ? details.getRiskScore() : 0.0;
                        int newVulnCount = (details.getVulnerabilities() != null) ? details.getVulnerabilities().size() : 0;

                        // Update only if there are changes
                        if (pkg.getRiskScore() != newScore || pkg.getVulnerabilitiesCount() != newVulnCount) {
                            pkg.setRiskScore(newScore);
                            pkg.setVulnerabilitiesCount(newVulnCount);
                            dataChanged = true;
                        }
                    }
                } catch (Exception ex) {
                    log.error("Error calculating risk for package {} in project {}", pkg.getName(), project.getName(), ex);
                }
            }
        }

        if (dataChanged) {
            try {
                project.setLastUpdate(Instant.now());
                projectRepository.save(project);
                log.info("[Project Listener] Risk metrics updated for project {}", projectName);
                return new Result.Success<>(null);
            } catch (Exception e) {
                log.error("Error saving updated risk metrics for project {}", projectName, e);
                return new Result.Failure<>(new DomainError.SystemError());
            }
        } else {
            log.debug("No risk changes detected for project {}", projectName);
            return new Result.Success<>(null);
        }
    }

    /* Utility methods */
    private boolean canEdit(String username, Project project) {
        return project.getAdmin().getUsername().equals(username) || 
               project.getCollaborators().stream().anyMatch(c -> c.getUsername().equals(username));
    }

    // We could implement a different logic in future
    private boolean canView(String username, Project project) {
        return canEdit(username, project); 
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

    private void triggerPostCreationEvents(Project project) {
        List<InstalledPackageDTO> dtos = project.getPackages().stream()
            .map(InstalledPackageDTO::fromEntity)
            .toList();
        eventPublisher.publishEvent(new ProjectEvent.CalculateRiskMetrics(project.getName(), dtos));

        List<InstalledPackageDTO> pkgList = project.getPackages().stream()
             .map(InstalledPackageDTO::fromEntity)
             .toList();
        eventPublisher.publishEvent(new ProjectEvent.ProjectCreated(project.getName(), project.getAdmin().getUsername(), pkgList));
    }

    private void triggerPostPackageUpdateEvents(Project project) {
        List<InstalledPackageDTO> dtos = project.getPackages().stream()
            .map(InstalledPackageDTO::fromEntity)
            .toList();
        eventPublisher.publishEvent(new ProjectEvent.CalculateRiskMetrics(project.getName(), dtos));

        List<InstalledPackageDTO> pkgList = project.getPackages().stream()
             .map(InstalledPackageDTO::fromEntity)
             .toList();
        eventPublisher.publishEvent(new ProjectEvent.ProjectPackagesUpdated(project.getName(), pkgList));
    }

    private Result<Void> changeCollaboratorData(String projectName, String collaboratorUsername, String newUsername, String newEmail) {
        var projectOpt = projectRepository.findByName(projectName);
        if (projectOpt.isEmpty()) {
            return new Result.Failure<>(new DomainError.NotFound("Project not found"));
        }

        Project project = projectOpt.get();
        var collaborator = project.getCollaborators();
        collaborator.stream()
                    .filter(c -> c.getUsername().equals(collaboratorUsername))
                    .findFirst() 
                    .ifPresent(c -> {
                        if (newUsername != null && !newUsername.isBlank()) c.setUsername(newUsername);
                        if (newEmail != null && !newEmail.isBlank()) c.setEmail(newEmail);
                    });
        
        if (project.getAdmin().getUsername().equals(collaboratorUsername)) {
            if (newUsername != null && !newUsername.isBlank()) {
                project.getAdmin().setUsername(newUsername);
            }    

            if (newEmail != null && !newEmail.isBlank()) {
                project.getAdmin().setEmail(newEmail);
            }    
        }

        projectRepository.save(project);
        return new Result.Success<>(null);
    }

}