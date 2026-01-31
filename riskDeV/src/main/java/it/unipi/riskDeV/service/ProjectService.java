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

import it.unipi.riskDeV.DTO.MessageResponseDTO;
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

    
    public Result<ProjectDTO> getProject(String projectName) {
        var projectOpt = projectRepository.findByName(projectName);
        
        if (projectOpt.isEmpty()) {
            return new Result.Failure<>(new DomainError.NotFound("Project not Found"));
        }
        
        Project project = projectOpt.get();

        if (!canView(getCurrentUsername(), project)) {
            return new Result.Failure<>(new DomainError.AccessDenied("You are not authorized to view this project."));
        }

        return new Result.Success<>(ProjectDTO.fromEntity(project));
    }

    @Transactional
    public Result<ProjectDTO> addProject(ProjectCreationDTO dto) {
        if (projectRepository.existsByName(dto.getName())) {
            return new Result.Failure<>(new DomainError.AlreadyExists("Project " + dto.getName() + " already exists."));
        }

        String username = getCurrentUsername();
        Project.Collaborator admin = getCollaboratorInfo(username);

        List<Project.ProjectPackage> packages = dto.getPackages().stream()
                .map(pkgDto -> new Project.ProjectPackage(pkgDto.getName(), pkgDto.getVersion()))
                .toList();

        Project project = new Project(dto.getName(), dto.getDescription(), dto.getPythonVersion(), admin, packages);
        Project savedProject = projectRepository.save(project);

        triggerPostCreationEvents(savedProject);
        return new Result.Success<>(ProjectDTO.fromEntity(savedProject));
    }

    @Transactional
    public Result<MessageResponseDTO> deleteProject(String projectName) {
        var projectOpt = projectRepository.findByName(projectName);
        
        if (projectOpt.isEmpty()) {
            return new Result.Failure<>(new DomainError.NotFound("Project not found"));
        }
        
        Project project = projectOpt.get();

        if (!project.getAdmin().getUsername().equals(getCurrentUsername())) {
            return new Result.Failure<>(new DomainError.AccessDenied("Only the owner can delete the project"));
        }

        List<String> userIds = project.getCollaborators().stream()
                .map(Project.Collaborator::getUsername)
                .toList();

        projectRepository.delete(project);

        eventPublisher.publishEvent(new ProjectEvent.ProjectDeleted(projectName, userIds));
        return new Result.Success<>(new MessageResponseDTO("Deleted project " + projectName + "."));
    }

    @Transactional
    public Result<MessageResponseDTO> updateProjectPackages(String projectName, List<InstalledPackageDTO> newPackages) {
        if (newPackages == null || newPackages.isEmpty()) {
            return new Result.Failure<>(new DomainError.ValidationFailed("Package list cannot be null or empty."));
        }

        var projectOpt = projectRepository.findByName(projectName);
        if (projectOpt.isEmpty()) {
            return new Result.Failure<>(new DomainError.NotFound("Project not found"));
        }
        Project project = projectOpt.get();

        if (!canEdit(getCurrentUsername(), project)) {
            return new Result.Failure<>(new DomainError.AccessDenied("Permission denied"));
        }

        List<String> missingPackages = new ArrayList<>();
        for (InstalledPackageDTO pkg : newPackages) {
            if (packageService.getPackageByNameVersion(pkg.getName(), pkg.getVersion()) instanceof Result.Failure) {
                missingPackages.add(pkg.getName() + "@" + pkg.getVersion());
            }
        }

        if (!missingPackages.isEmpty()) {
            return new Result.Failure<>(new DomainError.ValidationFailed("The following packages are not in the system: " + String.join(", ", missingPackages)));
        }

        Set<String> newPackageNames = newPackages.stream()
            .map(InstalledPackageDTO::getName)
            .collect(Collectors.toSet());
        
        project.getPackages().removeIf(p -> newPackageNames.contains(p.getName()));

        List<Project.ProjectPackage> pkgsToAdd = newPackages.stream()
            .map(dto -> new Project.ProjectPackage(dto.getName(), dto.getVersion()))
            .toList();
        
        project.getPackages().addAll(pkgsToAdd);
        project.setLastUpdate(Instant.now()); 

        Project savedProject = projectRepository.save(project);

        triggerPostPackageUpdateEvents(savedProject);

        return new Result.Success<>(new MessageResponseDTO("Packages updated for " + projectName + "."));
    }

    @Transactional
    public Result<MessageResponseDTO> removePackagesFromProject(String projectName, List<String> packagesToRemove) {
        var projectOpt = projectRepository.findByName(projectName);
        if (projectOpt.isEmpty()) {
            return new Result.Failure<>(new DomainError.NotFound("Project not found"));
        }
        Project project = projectOpt.get();

        if (!canEdit(getCurrentUsername(), project)) {
            return new Result.Failure<>(new DomainError.AccessDenied("Permission denied"));
        }

        boolean changed = project.getPackages().removeIf(p -> 
            packagesToRemove.contains(p.getName())
        );

        if (!changed) {
            return new Result.Failure<>(new DomainError.NotFound("No packages matched for removal."));
        }

        project.setLastUpdate(Instant.now());
        projectRepository.save(project);

        triggerPostPackageUpdateEvents(project);

        return new Result.Success<>(new MessageResponseDTO("Packages removed successfully."));
    }

    @Transactional
    public Result<MessageResponseDTO> addCollaboratorToProject(String projectName, String collaboratorUsername) {
        var projectOpt = projectRepository.findByName(projectName);
        if (projectOpt.isEmpty()) return new Result.Failure<>(new DomainError.NotFound("Project not found"));
        Project project = projectOpt.get();

        if (!project.getAdmin().getUsername().equals(getCurrentUsername())) {
            return new Result.Failure<>(new DomainError.AccessDenied("Only owner can add collaborators"));
        }

        boolean alreadyExists = project.getAdmin().getUsername().equals(collaboratorUsername) || 
                                project.getCollaborators().stream().anyMatch(c -> c.getUsername().equals(collaboratorUsername));
    
        if (alreadyExists) {
            return new Result.Failure<>(new DomainError.InvalidOperation("User is already associated with this project"));
        }

        Project.Collaborator newCollab;
        try {
            newCollab = getCollaboratorInfo(collaboratorUsername);
        } catch (IllegalStateException e) {
            return new Result.Failure<>(new DomainError.NotFound(e.getMessage()));
        }

        project.getCollaborators().add(newCollab);
        project.setLastUpdate(Instant.now());
            
        projectRepository.save(project);
            
        eventPublisher.publishEvent(new ProjectEvent.CollaboratorAdded(projectName, collaboratorUsername));
        return new Result.Success<>(new MessageResponseDTO("Collaborator " + collaboratorUsername + " added"));
    }

    @Transactional
    public Result<MessageResponseDTO> removeCollaboratorFromProject(String projectName, String collaboratorUsername) {
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
            return new Result.Failure<>(new DomainError.InvalidOperation("Owner cannot be removed. Transfer ownership."));
        }

        boolean removed = project.getCollaborators().removeIf(c -> c.getUsername().equals(collaboratorUsername));
        
        if (removed) {
            project.setLastUpdate(Instant.now());
            projectRepository.save(project);

            eventPublisher.publishEvent(new ProjectEvent.CollaboratorRemoved(projectName, collaboratorUsername));
            return new Result.Success<>(new MessageResponseDTO("Removed collaborator " + collaboratorUsername + " from project " + projectName + "."));
        } 
    
        return new Result.Failure<>(new DomainError.NotFound("Collaborator not found"));
    }

    @Transactional
    public Result<MessageResponseDTO> leaveProject(String projectName) {
        String requester = getCurrentUsername();
        return removeCollaboratorFromProject(projectName, requester);
    }

    @Transactional
    public Result<MessageResponseDTO> transferOwnership(String projectName, String newAdminUsername) {
        var projectOpt = projectRepository.findByName(projectName);
        if (projectOpt.isEmpty()) return new Result.Failure<>(new DomainError.NotFound("Project not found"));
        Project project = projectOpt.get();

        if (!project.getAdmin().getUsername().equals(getCurrentUsername())) {
            return new Result.Failure<>(new DomainError.AccessDenied("Only the current owner can transfer ownership"));
        }

        boolean isCollaborator = project.getCollaborators().stream()
                .anyMatch(c -> c.getUsername().equals(newAdminUsername));

        if (!isCollaborator) {
            return new Result.Failure<>(new DomainError.InvalidOperation("New owner must be an existing collaborator"));
        }

        Project.Collaborator oldAdmin = project.getAdmin();
        Project.Collaborator newAdmin = project.getCollaborators().stream()
                .filter(c -> c.getUsername().equals(newAdminUsername))
                .findFirst().get();

        project.getCollaborators().remove(newAdmin);
        project.getCollaborators().add(oldAdmin);
        project.setAdmin(newAdmin);
        
        project.setLastUpdate(Instant.now());
        projectRepository.save(project);

        return new Result.Success<>(new MessageResponseDTO("Ownership transferred to " + newAdminUsername));
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

    // The following 3 methods are used only by async functionalities
    @Transactional
    public void changeCollaboratorDataInProjects(List<String> projectNames, String collaboratorUsername, String newUsername, String newEmail) {
        for (String projectName : projectNames) {
            Result<Void> result = changeCollaboratorData(projectName, collaboratorUsername, newUsername, newEmail);

            if (result instanceof Result.Failure<Void> failure) {
                throw new RuntimeException("Failed to update user in project " + projectName + ": " + failure.error().message());
            }
        }
    }

    @Transactional
    public void removeCollaboratorFromProjects(List<String> projectsName, String username) {
        for (String pName : projectsName) {
            Result<MessageResponseDTO> result = removeCollaboratorFromProject(pName, username);

            if (result instanceof Result.Failure<?> failure) {
                throw new RuntimeException("Failed to remove user from project " + pName + ": " + failure.error().message());
            }
        }
    }

    @Transactional
    public void updateRiskMetrics(String projectName) {
        var projectOpt = projectRepository.findByName(projectName);
        if (projectOpt.isEmpty()) {
            throw new RuntimeException("Project " + projectName + " not found during risk calculation.");
        }

        Project project = projectOpt.get();
        boolean dataChanged = false;

        if (project.getPackages() != null) {
            for (Project.ProjectPackage pkg : project.getPackages()) {
                
                Result<PackageVersionDTO> result = packageService.getPackageByNameVersion(pkg.getName(), pkg.getVersion());

                if (result instanceof Result.Success<PackageVersionDTO> success) {
                    PackageVersionDTO details = success.data();
                    
                    double newScore = (details.getRiskScore() != null) ? details.getRiskScore() : 0.0;
                    int newVulnCount = (details.getVulnerabilities() != null) ? details.getVulnerabilities().size() : 0;

                    if (pkg.getRiskScore() != newScore || pkg.getVulnerabilitiesCount() != newVulnCount) {
                        pkg.setRiskScore(newScore);
                        pkg.setVulnerabilitiesCount(newVulnCount);
                        dataChanged = true;
                    }
                } else if (result instanceof Result.Failure<?> failure) {
                    throw new RuntimeException("Failed to fetch risk data for package " + pkg.getName() + ": " + failure.error().message());
                }
            }
        }

        if (dataChanged) {
            project.setLastUpdate(Instant.now());
            
            projectRepository.save(project);
            log.info("Risk metrics updated for project {}", projectName);
        } else {
            log.debug("No risk changes detected for project {}", projectName);
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