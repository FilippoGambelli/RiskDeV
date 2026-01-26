package it.unipi.riskDeV.listener;

import java.time.Instant;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.unipi.riskDeV.DTO.PackageVersionDTO;
import it.unipi.riskDeV.common.DomainError;
import it.unipi.riskDeV.common.Result;
import it.unipi.riskDeV.event.ProjectEvents.CalculateRiskMetricsEvent;
import it.unipi.riskDeV.event.ProjectEvents.CollaboratorAddedEvent;
import it.unipi.riskDeV.event.ProjectEvents.CollaboratorRemovedEvent;
import it.unipi.riskDeV.event.ProjectEvents.ProjectCreatedEvent;
import it.unipi.riskDeV.event.ProjectEvents.ProjectDeletedEvent;
import it.unipi.riskDeV.model.FailedEvent;
import it.unipi.riskDeV.model.Project;
import it.unipi.riskDeV.repository.FailedEventRepository;
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
    private final FailedEventRepository failedEventRepository;
    private final ObjectMapper objectMapper;

    
    /************************/
    /* User-Project Syncing */
    /************************/

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProjectCreated(ProjectCreatedEvent event) {
        log.info("[Project Listener] Project created: {}. Syncing admin profile for: {}", event.projectName(), event.adminUsername());
        
        var result = userService.addProjectToUser(event.adminUsername(), event.projectName());
        if (result instanceof Result.Failure<?> failure) {
            log.error("Error syncing admin profile for project {}", event.projectName(), failure.error());
            saveToDLQ(event, failure.error());
        } else {
            log.debug("Successfully synced admin profile for project {}", event.projectName());
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProjectDeleted(ProjectDeletedEvent event) {
        log.info("[Project Listener] Project deleted: {}. Cleaning involved users profiles.", event.projectName());

        if (event.involvedCollaborators() == null || event.involvedCollaborators().isEmpty()) {
            log.debug("No collaborators to clean for project {}", event.projectName());
            return;
        }

        for (String username : event.involvedCollaborators()) {
            var result = userService.removeProjectFromUser(username, event.projectName());
            if (result instanceof Result.Failure<?> failure) {
                log.error("Failed to remove project from user {}. Saving to DLQ. Error: {}", username, failure.error().message());
                saveToDLQ(new CollaboratorRemovedEvent(event.projectName(), username), failure.error());
            } else {
                log.debug("Successfully removed project from user {}", username);
            }
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCollaboratorAdded(CollaboratorAddedEvent event) {
        log.debug("[Project Listener] Collaborator {} added to {}. Updating user profile.", event.collaboratorUsername(), event.projectName());
        
        var result = userService.addProjectToUser(event.collaboratorUsername(), event.projectName());
        if (result instanceof Result.Failure<?> failure) {
            log.error("Failed to add project to collaborator {}. Saving to DLQ. Error: {}", event.collaboratorUsername(), failure.error().message());
            saveToDLQ(event, failure.error());
        } else {
            log.debug("Successfully added project to collaborator {}", event.collaboratorUsername());
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCollaboratorRemoved(CollaboratorRemovedEvent event) {
        log.debug("[Project Listener] Collaborator {} removed from {}. Updating user profile.", event.collaboratorUsername(), event.projectName());
        
        var result = userService.removeProjectFromUser(event.collaboratorUsername(), event.projectName());
        if (result instanceof Result.Failure<?> failure) {
            log.error("Failed to remove project from collaborator {}. Saving to DLQ. Error: {}", event.collaboratorUsername(), failure.error().message());
            saveToDLQ(event, failure.error());
        } else {
            log.debug("Successfully removed project from collaborator {}", event.collaboratorUsername());
        }
    }


    /****************************/
    /* Risk Metrics Calculation */
    /****************************/

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCalculateRiskMetrics(CalculateRiskMetricsEvent event) {
        log.debug("[Project Listener] Calculating risk metrics for project: {}", event.projectName());
        
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
            try {
                project.setLastUpdate(Instant.now());
                projectRepository.save(project);
                log.info("[Project Listener] Risk metrics updated for project {}", event.projectName());
            } catch (Exception e) {
                log.error("Error saving updated risk metrics for project {}", event.projectName(), e);
                saveToDLQ(event, new DomainError.SystemError("Failed to save updated risk metrics", e));
                return;
            }
        } else {
            log.debug("No risk changes detected for project {}", event.projectName());
        }
    }

     private void saveToDLQ(Object event, DomainError e) {
        try {
            FailedEvent dlq = new FailedEvent();
            dlq.setEventType(event.getClass().getSimpleName());
            dlq.setPayloadJson(objectMapper.writeValueAsString(event));
            dlq.setExceptionMessage(e.message());
            dlq.setFailedAt(Instant.now());
            dlq.setRetryCount(0);

            failedEventRepository.save(dlq);
            log.info("Event {} saved to DLQ", event.getClass().getSimpleName());
            
        } catch (Exception jsonEx) {
            log.error("Failed to serialize event {} for DLQ", event.getClass().getSimpleName(), jsonEx);
        }
    }
}
