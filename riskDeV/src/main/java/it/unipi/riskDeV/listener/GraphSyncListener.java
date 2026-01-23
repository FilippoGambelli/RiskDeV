package it.unipi.riskDeV.listener;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.unipi.riskDeV.event.ProjectEvents.CollaboratorAddedEvent;
import it.unipi.riskDeV.event.ProjectEvents.CollaboratorRemovedEvent;
import it.unipi.riskDeV.event.ProjectEvents.ProjectCreatedEvent;
import it.unipi.riskDeV.event.ProjectEvents.ProjectDeletedEvent;
import it.unipi.riskDeV.event.ProjectEvents.ProjectPackagesUpdatedEvent;
import it.unipi.riskDeV.event.UserEvent;
import it.unipi.riskDeV.model.FailedEvent;
import it.unipi.riskDeV.repository.FailedEventRepository;
import it.unipi.riskDeV.service.GraphService;
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
    public void handleUserDeleted(UserEvent.UserDeletedEvent event) {
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
    public void handleUserCreated(UserEvent.UserCreatedEvent event) {
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
    public void handleUserUpdated(UserEvent.UserUpdatedEvent event) {
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
                event.adminId(), 
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
            log.debug("Neo4j: Adding collaborator {} to {}", event.userId(), event.projectName());
            graphService.addCollaborator(event.projectName(), event.userId());
        } catch (Exception e) {
            log.error("Neo4j Sync Failed: Add Collaborator", e);
            saveToDLQ(event, e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleCollaboratorRemoved(CollaboratorRemovedEvent event) {
        try {
            log.debug("Neo4j: Removing collaborator {} from {}", event.userId(), event.projectName());
            graphService.removeCollaborator(event.projectName(), event.userId());
        } catch (Exception e) {
            log.error("Neo4j Sync Failed: Remove Collaborator", e);
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