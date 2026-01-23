package it.unipi.riskDeV.service;

import it.unipi.riskDeV.repository.ProjectGraphRepository;
import it.unipi.riskDeV.repository.UserGraphRepository; 
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class GraphService {

    private final UserGraphRepository userGraphRepository;
    private final ProjectGraphRepository projectGraphRepository;

    public void deleteUserNode(String id) {
        log.debug("Syncing delete to Neo4j for user: {}", id);
        userGraphRepository.deleteById(id);
    }

    public void createUserNode(String username) {
        log.debug("Syncing create to Neo4j for user: {}", username);
        userGraphRepository.createUserNode(username);
    }

    public void updateUsername(String oldUsername, String newUsername) {
        log.debug("Syncing username update to Neo4j for user mongoId: {} with new username: {}", oldUsername, newUsername);
        userGraphRepository.updateUsername(oldUsername, newUsername);
    }

    public void createProjectStructure(String projectName, String adminId, List<String> packageIds) {
        projectGraphRepository.createProjectNode(projectName);
    
        if (adminId != null) {
            projectGraphRepository.setProjectOwner(projectName, adminId);
        }

        if (packageIds != null && !packageIds.isEmpty()) {
            projectGraphRepository.replaceAllDependencies(projectName, packageIds);
        }
    }

    public void deleteProjectNode(String projectName) {
        projectGraphRepository.deleteProjectByName(projectName);
    }

    public void syncProjectPackages(String projectName, List<String> packageIds) {
        projectGraphRepository.replaceAllDependencies(projectName, packageIds);
    }

    public void addCollaborator(String projectName, String userId) {
        projectGraphRepository.addCollaboratorRelation(projectName, userId);
    }

    public void removeCollaborator(String projectName, String userId) {
        projectGraphRepository.removeCollaboratorRelation(projectName, userId);
    }
}