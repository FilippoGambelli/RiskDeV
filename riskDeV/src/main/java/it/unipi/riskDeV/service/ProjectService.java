package it.unipi.riskDeV.service;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.springframework.dao.DataAccessException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import it.unipi.riskDeV.DTO.InstalledPackageDTO;
import it.unipi.riskDeV.DTO.ProjectDTO;
import it.unipi.riskDeV.common.DomainError;
import it.unipi.riskDeV.common.Result;
import it.unipi.riskDeV.exception.ProjectNotFoundException;
import it.unipi.riskDeV.repository.ProjectGraphRepository;
import it.unipi.riskDeV.repository.UserGraphRepository;
import it.unipi.riskDeV.model.neo4j.ProjectNode;
import it.unipi.riskDeV.model.neo4j.UserNode;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectService {

    private final UserGraphRepository userGraphRepository;
    private final ProjectGraphRepository projectGraphRepository;

    public Result<List<String>> getAllUserProjects(String userId) {
        try {
            List<String> projectIds = projectGraphRepository.findProjectIdsByUserId(userId);

            if (projectIds == null) {
                return new Result.Success<>(Collections.emptyList());
            }
            return new Result.Success<>(projectIds);

        } catch (DataAccessException e) {
            log.error("Error retrieving projects for user {}", userId, e);
            return new Result.Failure<>(new DomainError.SystemError("Failed to retrieve user projects.", e));
        }
    }

    @Transactional
    public Result<String> insertProject(String userId, ProjectDTO projectDTO) {
        log.info("Starting insert transaction for user: {}", userId);

        try {
            ProjectNode projectNode = new ProjectNode();
            
            String projectId = UUID.randomUUID().toString();
            projectNode.setId(projectId);
            
            projectNode.setName(projectDTO.getName());
            projectNode.setDescription(projectDTO.getDescription());
            projectNode.setPythonVersion(projectDTO.getPythonVersion());
            projectNode.setLastUpdate(Instant.now());

            UserNode userNode = new UserNode();
            userNode.setId(userId);
            projectNode.setOwner(userNode);

            if (projectDTO.getPackages() != null && !projectDTO.getPackages().isEmpty()) {
                linkPackagesToProject(projectNode, projectDTO.getPackages());
            }

            projectGraphRepository.save(projectNode);
            log.info("Project created successfully on Graph with ID: {}", projectId);
            return new Result.Success<>(projectId);

        } catch (DataAccessException e) {
            log.error("Database error during project creation for user {}", userId, e);
            return new Result.Failure<>(new DomainError.SystemError("Failed to create project.", e));
        }
    }

    @Transactional
    @PreAuthorize("@projectGraphRepository.isUserOwner(#userId, #projectId)")
    public Result<String> deleteProject(String userId, String projectId) {
        log.info("Requesting deletion of project {} for user: {}", projectId, userId);

        if (!projectGraphRepository.existsById(projectId)) {
             throw new ProjectNotFoundException("Project with ID " + projectId + " not found.");
        }

        try {
            projectGraphRepository.deleteById(projectId);
            log.info("Project {} deleted from Graph.", projectId);
            return new Result.Success<>(projectId);

        } catch (DataAccessException e) {
            log.error("Failed to delete project {} from Graph", projectId, e);
            return new Result.Failure<>(new DomainError.SystemError("Delete failed.", e));
        }
    }

    @Transactional
    public Result<String> updateProjectPackages(String userId, String projectId, List<InstalledPackageDTO> newPackages) {

        if (newPackages == null || newPackages.isEmpty()) {
            return new Result.Failure<>(new DomainError.ValidationFailed("Package list cannot be null or empty."));
        }
        
        log.info("Adding packages to project {} for user {}", projectId, userId);

        // var is like auto in C++
        var projectOpt = projectGraphRepository.findById(projectId);
        if (projectOpt.isEmpty()) {
            return new Result.Failure<>(new DomainError.NotFound("Project with ID " + projectId + " not found."));
        }

        try {
            ProjectNode graphProject = projectOpt.get();
            graphProject.setLastUpdate(Instant.now());
            linkPackagesToProject(graphProject, newPackages);
            projectGraphRepository.save(graphProject);

            log.info("Project structure updated for project {}", projectId);
            return new Result.Success<>(projectId);

        } catch (DataAccessException e) {
            log.error("Error updating packages for project {}", projectId, e);
            return new Result.Failure<>(new DomainError.SystemError("Failed to update project packages.", e));
        }
    }

    @Transactional
    public Result<String> removePackagesFromProject(String userId, String projectId, List<InstalledPackageDTO> packages) {
        log.info("Removing packages {} from project {}", packages, projectId);

        if (!projectGraphRepository.existsById(projectId)) {
            return new Result.Failure<>(new DomainError.NotFound("Project with ID " + projectId + " not found."));
        }

        try {
            List<String> idsToRemove = packages.stream()
                .map(dto -> dto.getName() + " " + dto.getVersion())
                .toList();

            projectGraphRepository.removeDependenciesByName(projectId, idsToRemove);
            projectGraphRepository.updateLastUpdateTimestamp(projectId, Instant.now());

            log.info("Dependencies removed from graph for project {}", projectId);
            return new Result.Success<>(projectId);

        } catch (DataAccessException e) {
            log.error("Failed to remove packages from graph for project {}", projectId, e);
            return new Result.Failure<>(new DomainError.SystemError("Failed to remove packages from project.", e));
        }
    }

    @Transactional 
    @PreAuthorize("@projectGraphRepository.isUserOwner(#userId, #projectId)")
    public Result<String> addCollaboratorToProject(String userId, String projectId, String collaboratorUserId) {
        log.info("Request to add collaborator {} to project {} by user {}", collaboratorUserId, projectId, userId);

        if (!projectGraphRepository.existsById(projectId)) {
            return new Result.Failure<>(new DomainError.NotFound("Project with ID " + projectId + " not found."));
        }

        if (!userGraphRepository.existsById(collaboratorUserId)) {
            return new Result.Failure<>(new DomainError.NotFound("User with ID " + collaboratorUserId + " not found."));
        }

        // The owner can't be a collaborator
        if (projectGraphRepository.isUserOwner(collaboratorUserId, projectId)) {
            return new Result.Failure<>(new DomainError.InvalidOperation("User " + collaboratorUserId + " is already the owner of this project."));
        }
        
        try {
            projectGraphRepository.addCollaborator(projectId, collaboratorUserId);
            log.info("Added collaborator {} to project {}", collaboratorUserId, projectId);
            return new Result.Success<>("Collaborator added successfully.");

        } catch (DataAccessException e) {
            log.error("Error adding collaborator {} to project {}", collaboratorUserId, projectId, e);
            return new Result.Failure<>(new DomainError.SystemError("Failed to add collaborator.", e));
        }
    }

    @Transactional
    public Result<List<String>> getProjectCollaborators(String userId, String projectId) {

        if (!projectGraphRepository.existsById(projectId)) {
            return new Result.Failure<>(new DomainError.NotFound("Project with ID " + projectId + " not found."));
        }

        try {
            boolean isParticipant = projectGraphRepository.findProjectIdsByUserId(userId).contains(projectId);
            if (!isParticipant) {
                return new Result.Failure<>(new DomainError.AccessDenied("You are not a collaborator of this project."));
            }

            return new Result.Success<>(projectGraphRepository.findCollaboratorsByProjectId(projectId));

        } catch (DataAccessException e) {
            log.error("Error fetching collaborators for project {}", projectId, e);
            return new Result.Failure<>(new DomainError.SystemError("Error retrieving collaborators", e));
        }
    }

    @Transactional
    @PreAuthorize("@projectGraphRepository.isUserOwner(#userId, #projectId)")
    public Result<String> removeCollaboratorFromProject(String userId, String projectId, String collaboratorUserId) {
        log.info("Removing collaborator {} from project {} requested by {}", collaboratorUserId, projectId, userId);

        if (!projectGraphRepository.existsById(projectId)) {
            return new Result.Failure<>(new DomainError.NotFound("Project " + projectId + " not found"));
        }

        boolean isRequesterOwner = projectGraphRepository.isUserOwner(userId, projectId);
        boolean isSelfRemoval = userId.equals(collaboratorUserId); 

        if (!isRequesterOwner) {
            return new Result.Failure<>(new DomainError.NotFound("You are not authorized to remove collaborators."));
        }

        if (isSelfRemoval) {
            return new Result.Failure<>(new DomainError.InvalidOperation("You are the project's owner."));
        }

        try {
            projectGraphRepository.removeCollaborator(projectId, collaboratorUserId);
            
            log.info("Removed collaborator {} from project {}", collaboratorUserId, projectId);
            return new Result.Success<>(userId);

        } catch (DataAccessException e) {
            log.error("Error removing collaborator {} from project {}", collaboratorUserId, projectId, e);
            return new Result.Failure<>(new DomainError.SystemError("Error removing collaborator.", e));
        }
    }

    // Utility method
    private void linkPackagesToProject(ProjectNode project, List<InstalledPackageDTO> pkgs) {
        List<String> versionIds = pkgs.stream()
                .map(pkg -> pkg.getName() + " " + pkg.getVersion())
                .toList();

        projectGraphRepository.addDependenciesToProject(project.getId(), versionIds);
    }

}
