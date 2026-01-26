package it.unipi.riskDeV.listener;

import java.time.Instant;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import it.unipi.riskDeV.DTO.packageVersion.PackageVersionDTO;
import it.unipi.riskDeV.common.Result;
import it.unipi.riskDeV.event.ProjectEvents.CalculateRiskMetricsEvent;
import it.unipi.riskDeV.event.ProjectEvents.CollaboratorAddedEvent;
import it.unipi.riskDeV.event.ProjectEvents.CollaboratorRemovedEvent;
import it.unipi.riskDeV.event.ProjectEvents.ProjectCreatedEvent;
import it.unipi.riskDeV.event.ProjectEvents.ProjectDeletedEvent;
import it.unipi.riskDeV.model.Project;
import it.unipi.riskDeV.repository.ProjectRepository;
import it.unipi.riskDeV.service.PackageService;
import it.unipi.riskDeV.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProjectListener {

    private final UserService userService;
    private final PackageService packageService;
    private final ProjectRepository projectRepository;

    
    /************************/
    /* User-Project Syncing */
    /************************/

    @Async
    @EventListener
    public void onProjectCreated(ProjectCreatedEvent event) {
        log.info("[Domain Listener] Project created: {}. Syncing admin profile for: {}", event.projectName(), event.adminUsername());
        try {
            userService.addProjectToUser(event.adminUsername(), event.projectName());
        } catch (Exception e) {
            log.error("Error syncing admin profile for project {}", event.projectName(), e);
        }
    }

    @Async
    @EventListener
    public void onProjectDeleted(ProjectDeletedEvent event) {
        log.info("[Domain Listener] Project deleted: {}. Cleaning involved users profiles.", event.projectName());

        if (event.involvedCollaborators() == null || event.involvedCollaborators().isEmpty()) {
            return;
        }

        for (String username : event.involvedCollaborators()) {
            try {
                userService.removeProjectFromUser(username, event.projectName());
            } catch (Exception e) {
                log.warn("Failed to remove project ref from user {}", username, e);
            }
        }
    }

    @Async
    @EventListener
    public void onCollaboratorAdded(CollaboratorAddedEvent event) {
        log.debug("[Domain Listener] Collaborator {} added to {}. Updating user profile.", event.collaboratorUsername(), event.projectName());
        try {
            userService.addProjectToUser(event.collaboratorUsername(), event.projectName());
        } catch (Exception e) {
            log.error("Error adding project ref to collaborator {}", event.collaboratorUsername(), e);
        }
    }

    @Async
    @EventListener
    public void onCollaboratorRemoved(CollaboratorRemovedEvent event) {
        log.debug("[Domain Listener] Collaborator {} removed from {}. Updating user profile.", event.collaboratorUsername(), event.projectName());
        try {
            userService.removeProjectFromUser(event.collaboratorUsername(), event.projectName());
        } catch (Exception e) {
            log.error("Error removing project ref from collaborator {}", event.collaboratorUsername(), e);
        }
    }


    /****************************/
    /* Risk Metrics Calculation */
    /****************************/

    @Async
    @EventListener
    public void onCalculateRiskMetrics(CalculateRiskMetricsEvent event) {
        log.info("[Domain Listener] Calculating risk metrics for project: {}", event.projectName());
        
        var projectOpt = projectRepository.findByName(event.projectName());
        if (projectOpt.isEmpty()) {
            log.warn("Project {} not found during risk calculation. Skipping.", event.projectName());
            return;
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
            project.setLastUpdate(Instant.now());
            projectRepository.save(project);
            log.info("[Domain Listener] Risk metrics updated for project {}", event.projectName());
        } else {
            log.debug("[Domain Listener] No risk changes detected for project {}", event.projectName());
        }
    }
}
