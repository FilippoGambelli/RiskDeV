package it.unipi.riskDeV.service;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.springframework.dao.DataAccessException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import it.unipi.riskDeV.DTO.InstalledPackageDTO;
import it.unipi.riskDeV.DTO.ProjectDTO;
import it.unipi.riskDeV.exception.ProjectNotFoundException;
import it.unipi.riskDeV.exception.ServiceException;
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
@PreAuthorize("hasRole('ROLE_USER')")
public class ProjectService {

    private final UserGraphRepository userGraphRepository;
    private final ProjectGraphRepository projectGraphRepository;

    public List<String> getAllUserProjects() {
        String userId = getCurrentUser();
        try {
            List<String> projectIds = projectGraphRepository.findProjectIdsByUserId(userId);

            if (projectIds == null) {
                return Collections.emptyList();
            }
            return projectIds;

        } catch (DataAccessException e) {
            log.error("Error retrieving projects for user {}", userId, e);
            throw new ServiceException("Impossible to retrieve projects for the user.");
        }
    }

    @Transactional
    public String insertProject(ProjectDTO projectDTO) {
        String userId = getCurrentUser();
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
            return projectId;

        } catch (DataAccessException e) {
            log.error("Database error during project creation for user {}", userId, e);
            throw new ServiceException("Impossible to create the project.");
        }
    }

    @Transactional
    public String deleteProject(String projectId) {
        String userId = getCurrentUser();
        log.info("Requesting deletion of project {} for user: {}", projectId, userId);

        if (!projectGraphRepository.existsById(projectId)) {
             throw new ProjectNotFoundException("Project with ID " + projectId + " not found.");
        }

        if (!projectGraphRepository.isUserOwner(userId, projectId)) {
            throw new AccessDeniedException("User " + userId + " is not authorized to delete project " + projectId);
        }

        try {
            projectGraphRepository.deleteById(projectId);
            log.info("Project {} deleted from Graph.", projectId);
            return projectId;

        } catch (DataAccessException e) {
            log.error("Failed to delete project {} from Graph", projectId, e);
            throw new ServiceException("Failed to delete project.");
        }
    }

    @Transactional
    public String updateProjectPackages(String projectId, List<InstalledPackageDTO> newPackages) {
        String userId = getCurrentUser();

        if (newPackages == null || newPackages.isEmpty()) {
            throw new IllegalArgumentException("No packages provided to add to the project.");
        }
        
        log.info("Adding packages to project {} for user {}", projectId, userId);

        try {
            ProjectNode graphProject = projectGraphRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException("Project " + projectId + " not found."));

            graphProject.setLastUpdate(Instant.now());

            linkPackagesToProject(graphProject, newPackages);

            projectGraphRepository.save(graphProject);
            log.info("Project structure updated for project {}", projectId);
            return projectId;

        } catch (DataAccessException e) {
            log.error("Error updating packages for project {}", projectId, e);
            throw new ServiceException("Failed to update project packages.");
        }
    }

    @Transactional
    public String removePackagesFromProject(String projectId, List<InstalledPackageDTO> packages) {
        getCurrentUser();
        log.info("Removing packages {} from project {}", packages, projectId);

        try {
            if (!projectGraphRepository.existsById(projectId)) {
                 throw new ProjectNotFoundException("Project " + projectId + " not found.");
            }

            List<String> idsToRemove = packages.stream()
                .map(dto -> dto.getName() + " " + dto.getVersion())
                .toList();

            projectGraphRepository.removeDependenciesByName(projectId, idsToRemove);
            projectGraphRepository.updateLastUpdateTimestamp(projectId, Instant.now());

            log.info("Dependencies removed from graph for project {}", projectId);
            return projectId;

        } catch (DataAccessException e) {
            log.error("Failed to remove packages from graph for project {}", projectId, e);
            throw new ServiceException("Failed to remove packages from dependencies.");
        }
    }

    @Transactional 
    public String addCollaboratorToProject(String projectId, String collaboratorUserId) {
        String currentUserId = getCurrentUser();
        log.info("Request to add collaborator {} to project {} by user {}", collaboratorUserId, projectId, currentUserId);

        if (!projectGraphRepository.existsById(projectId)) {
            throw new ProjectNotFoundException("Project " + projectId + " not found");
        }

        if (!projectGraphRepository.isUserOwner(currentUserId, projectId)) {
            throw new AccessDeniedException("Only the project owner can add collaborators.");
        }

        if (!userGraphRepository.existsById(collaboratorUserId)) {
            throw new IllegalArgumentException("User" + collaboratorUserId + "is not in the system.");
        }

        // The owner can't be a collaborator
        if (projectGraphRepository.isUserOwner(collaboratorUserId, projectId)) {
            throw new IllegalArgumentException("User " + collaboratorUserId + " is already the owner of this project.");
        }
        
        try {
            projectGraphRepository.addCollaborator(projectId, collaboratorUserId);
            log.info("Added collaborator {} to project {}", collaboratorUserId, projectId);
            return "Collborator" + collaboratorUserId + "correctly added.";

        } catch (DataAccessException e) {
            log.error("Error adding collaborator {} to project {}", collaboratorUserId, projectId, e);
            throw new ServiceException("Failed to add collaborator due to database error.");
        }
    }

    @Transactional
    public List<String> getProjectCollaborators(String projectId) {
        String currentUserId = getCurrentUser();

        if (!projectGraphRepository.existsById(projectId)) {
             throw new ProjectNotFoundException("Project " + projectId + " not found");
        }

        try {
            boolean isParticipant = projectGraphRepository.findProjectIdsByUserId(currentUserId).contains(projectId);
            if (!isParticipant) {
                throw new AccessDeniedException("You are not part of this project.");
            }

            return projectGraphRepository.findCollaboratorsByProjectId(projectId);

        } catch (DataAccessException e) {
             log.error("Error fetching collaborators for project {}", projectId, e);
             throw new ServiceException("Error retrieving collaborators.");
        }
    }

    @Transactional
    public String removeCollaboratorFromProject(String projectId, String collaboratorUserId) {
        String currentUserId = getCurrentUser(); 
        log.info("Removing collaborator {} from project {} requested by {}", collaboratorUserId, projectId, currentUserId);

        if (!projectGraphRepository.existsById(projectId)) {
            throw new ProjectNotFoundException("Project " + projectId + " not found");
        }

        boolean isRequesterOwner = projectGraphRepository.isUserOwner(currentUserId, projectId);
        boolean isSelfRemoval = currentUserId.equals(collaboratorUserId); 

        if (!isRequesterOwner && !isSelfRemoval) {
            throw new AccessDeniedException("You are not authorized to remove this collaborator.");
        }

        try {
            projectGraphRepository.removeCollaborator(projectId, collaboratorUserId);
            
            log.info("Removed collaborator {} from project {}", collaboratorUserId, projectId);
            return collaboratorUserId;

        } catch (DataAccessException e) {
            log.error("Error removing collaborator {} from project {}", collaboratorUserId, projectId, e);
            throw new ServiceException("Error removing collaborator.");
        }
    }

    // Utility methods
    private String getCurrentUser() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private void linkPackagesToProject(ProjectNode project, List<InstalledPackageDTO> pkgs) {
        List<String> versionIds = pkgs.stream()
                .map(pkg -> pkg.getName() + " " + pkg.getVersion())
                .toList();

        projectGraphRepository.addDependenciesToProject(project.getId(), versionIds);
    }

}
