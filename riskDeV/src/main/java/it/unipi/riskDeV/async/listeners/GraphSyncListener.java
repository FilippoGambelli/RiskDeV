package it.unipi.riskDeV.async.listeners;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.unipi.riskDeV.async.GraphService;
import it.unipi.riskDeV.async.events.UserEvents;
import it.unipi.riskDeV.async.events.PackageEvents.DeletePackageVersionEvent;
import it.unipi.riskDeV.async.events.PackageEvents.UpdateDocumentationEvent;
import it.unipi.riskDeV.async.events.PackageEvents.UpdatePackageVersionEvent;
import it.unipi.riskDeV.async.events.PackageEvents.VersionReleaseEvent;
import it.unipi.riskDeV.async.events.ProjectEvents.CollaboratorAddedEvent;
import it.unipi.riskDeV.async.events.ProjectEvents.CollaboratorRemovedEvent;
import it.unipi.riskDeV.async.events.ProjectEvents.ProjectCreatedEvent;
import it.unipi.riskDeV.async.events.ProjectEvents.ProjectDeletedEvent;
import it.unipi.riskDeV.async.events.ProjectEvents.ProjectPackagesUpdatedEvent;
import it.unipi.riskDeV.async.events.VulnerabilityEvents.VulnerabilityCreatedEvent;
import it.unipi.riskDeV.async.events.VulnerabilityEvents.VulnerabilityDeletedEvent;
import it.unipi.riskDeV.async.events.VulnerabilityEvents.VulnerabilityUpdatedEvent;
import it.unipi.riskDeV.model.documentDB.FailedEvent;
import it.unipi.riskDeV.repository.documentDB.FailedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class GraphSyncListener {

    private final GraphService graphService;
    private final FailedEventRepository failedEventRepository;
    private final ObjectMapper objectMapper; // ObjectMapper to serialize event payloads for DLQ

    // Async listener to handle UserDeletedEvent after mongo transaction commit
    @Async 
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true) 
    public void handleUserDeleted(UserEvents.UserDeletedEvent event) {
        try {
            graphService.deleteUserNode(event.username());
            log.info("SUCCESS: User {} deleted from Neo4j.", event.username());
        } catch (Exception e) {
            log.error("FAIL: Could not delete user {} from Neo4j. Saving to DLQ.", event.username(), e);
            saveToDLQ(event, e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleUserCreated(UserEvents.UserCreatedEvent event) {
        try {            
            graphService.createUserNode(event.username());
            log.info("SUCCESS: UserNode {} created in Neo4j.", event.username());
        } catch (Exception e) {
            log.error("FAIL: Could not create user {} in Neo4j. Saving to DLQ.", event.username(), e);
            saveToDLQ(event, e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleUserUpdated(UserEvents.UserUpdatedEvent event) {
        try {
            graphService.updateUsername(event.oldUsername(), event.newUsername());
            log.info("SUCCESS: UserNode {} updated in Neo4j.", event.newUsername());
        } catch (Exception e) {
            log.error("FAIL: Could not update user {} in Neo4j. Saving to DLQ.", event.newUsername(), e);
            saveToDLQ(event, e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleProjectCreated(ProjectCreatedEvent event) {
        try {
            log.debug("Neo4j: Creating project {}", event.projectName());
            graphService.createProjectStructure(
                event.projectName(), 
                event.adminUsername(), 
                event.packageIds()
            );
        } catch (Exception e) {
            log.error("Neo4j Sync Failed: Create Project {}", event.projectName(), e);
            saveToDLQ(event, e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleProjectDeleted(ProjectDeletedEvent event) {
        try {
            log.debug("Neo4j: Deleting project {}", event.projectName());
            graphService.deleteProjectNode(event.projectName());
        } catch (Exception e) {
            log.error("Neo4j Sync Failed: Delete Project {}", event.projectName(), e);
            saveToDLQ(event, e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handlePackagesUpdated(ProjectPackagesUpdatedEvent event) {
        try {
            log.debug("Neo4j: Syncing packages for project {}", event.projectName());
            graphService.syncProjectPackages(event.projectName(), event.packageIds());
        } catch (Exception e) {
            log.error("Neo4j Sync Failed: Update Packages {}", event.projectName(), e);
            saveToDLQ(event, e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleCollaboratorAdded(CollaboratorAddedEvent event) {
        try {
            log.debug("Neo4j: Adding collaborator {} to {}", event.collaboratorUsername(), event.projectName());
            graphService.addCollaborator(event.projectName(), event.collaboratorUsername());
        } catch (Exception e) {
            log.error("Neo4j Sync Failed: Add Collaborator", e);
            saveToDLQ(event, e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleCollaboratorRemoved(CollaboratorRemovedEvent event) {
        try {
            log.debug("Neo4j: Removing collaborator {} from {}", event.collaboratorUsername(), event.projectName());
            graphService.removeCollaborator(event.projectName(), event.collaboratorUsername());
        } catch (Exception e) {
            log.error("Neo4j Sync Failed: Remove Collaborator", e);
            saveToDLQ(event, e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleVulnerabilityCreated(VulnerabilityCreatedEvent event) {
        try {
            log.debug("Neo4j: Adding vulnerability {}", event.cveId());
            graphService.addVulnerability(event.cveId(), event.description(), event.baseScore());
        } catch (Exception e) {
            log.error("Neo4j Sync Failed: Add vulnerability", e);
            saveToDLQ(event, e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleVulnerabilityUpdated(VulnerabilityUpdatedEvent event) {
        try {
            log.debug("Neo4j: Updating vulnerability {}", event.cveId());
            graphService.updateVulnerability(event.cveId(), event.description(), event.baseScore());
        } catch (Exception e) {
            log.error("Neo4j Sync Failed: Update vulnerability", e);
            saveToDLQ(event, e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleVulnerabilityDeteled(VulnerabilityDeletedEvent event) {
        try {
            log.debug("Neo4j: Deleting vulnerability {}", event.cveId());
            graphService.deleteVulnerability(event.cveId());
        } catch (Exception e) {
            log.error("Neo4j Sync Failed: Delete vulnerability", e);
            saveToDLQ(event, e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleVersionReleaseEvent(VersionReleaseEvent event) {
        try {
            log.debug("Neo4j: Adding package {}", event.publishedVersionDTO().getPackageName());
            graphService.addPackage(event.publishedVersionDTO());
        } catch (Exception e) {
            log.error("Neo4j Sync Failed: Adding package", e);
            saveToDLQ(event, e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleUpdateDocumentationEvent(UpdateDocumentationEvent event) {
        try {
            log.debug("Neo4j: Update documentation url for package {}", event.packageName());
            graphService.updatePackageDocumentation(event.packageName(), event.documentationURL());
        } catch (Exception e) {
            log.error("Neo4j Sync Failed: Updating documentation url", e);
            saveToDLQ(event, e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleUpdateDocumentationEvent(UpdatePackageVersionEvent event) {
        try {
            log.debug("Neo4j: Update package {}", event.packageName());
            graphService.updatePackageVersion(event.packageName(), event.version(), event.dependecies(), event.vulnerabilities());
        } catch (Exception e) {
            log.error("Neo4j Sync Failed: Updating package", e);
            saveToDLQ(event, e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleDeletePackageEvent(DeletePackageVersionEvent event) {
        try {
            log.debug("Neo4j: Delete package {}", event.packageName());
            graphService.deletePackageVersion(event.packageName(), event.version());
        } catch (Exception e) {
            log.error("Neo4j Sync Failed: Deleting package", e);
            saveToDLQ(event, e);
        }
    }

    private void saveToDLQ(Object event, Exception e) {
        try {
            FailedEvent dlq = new FailedEvent();
            dlq.setEventType(event.getClass().getSimpleName());
            dlq.setPayloadJson(objectMapper.writeValueAsString(event));
            dlq.setExceptionMessage(e.getMessage());
            dlq.setFailedAt(Instant.now());
            dlq.setRetryCount(0);

            failedEventRepository.save(dlq);
        } catch (Exception jsonEx) {
            log.error("CRITICAL: Failed to serialize event for DLQ", jsonEx);
        }
    }
}