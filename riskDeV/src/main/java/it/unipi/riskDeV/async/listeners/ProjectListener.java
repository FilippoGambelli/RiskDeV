package it.unipi.riskDeV.async.listeners;

import java.time.Instant;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.unipi.riskDeV.async.events.ProjectEvent;
import it.unipi.riskDeV.model.documentDB.FailedEvent;
import it.unipi.riskDeV.repository.documentDB.FailedEventRepository;
import it.unipi.riskDeV.results.DomainError;
import it.unipi.riskDeV.results.Result;
import it.unipi.riskDeV.service.ProjectService;
import it.unipi.riskDeV.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProjectListener {

    private final UserService userService;
    private final ProjectService projectService;
    private final FailedEventRepository failedEventRepository;
    private final ObjectMapper objectMapper;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleProjectEvent(ProjectEvent event) {
        log.info("[Project Listener] Processing {} for project: {}", event.getClass().getSimpleName(), event.projectName());

        try {
            switch (event) {
                case ProjectEvent.ProjectCreated c -> 
                    syncUserAction(c.adminUsername(), c.projectName(), true, c);

                case ProjectEvent.CollaboratorAdded a -> 
                    syncUserAction(a.collaboratorUsername(), a.projectName(), true, a);

                case ProjectEvent.CollaboratorRemoved r -> 
                    syncUserAction(r.collaboratorUsername(), r.projectName(), false, r);

                case ProjectEvent.ProjectDeleted d -> 
                    handleProjectDeletionCleanup(d);

                case ProjectEvent.CalculateRiskMetrics m -> 
                    handleRiskCalculation(m);

                case ProjectEvent.ProjectPackagesUpdated u -> {}
            };
        } catch (Exception e) {
            log.error("Unexpected error in ProjectListener for project {}", event.projectName(), e);
            saveToDLQ(event, new DomainError.SystemError());
        }
    }

    private void syncUserAction(String username, String projectName, boolean isAdd, ProjectEvent event) {
        var result = isAdd ? userService.addProjectToUser(username, projectName) : userService.removeProjectFromUser(username, projectName);
            
        if (result instanceof Result.Failure<?> failure) {
            log.error("Failed to {} project {} for user {}: {}", isAdd ? "add" : "remove", projectName, username, failure.error().message());
            saveToDLQ(event, failure.error());
        }
    }

    private void handleProjectDeletionCleanup(ProjectEvent.ProjectDeleted event) {
        if (event.involvedCollaborators() == null || event.involvedCollaborators().isEmpty()) return;

        for (String username : event.involvedCollaborators()) {
            var result = userService.removeProjectFromUser(username, event.projectName());
            if (result instanceof Result.Failure<?> failure) {
                saveToDLQ(new ProjectEvent.CollaboratorRemoved(event.projectName(), username), failure.error());
            }
        }
    }

    private void handleRiskCalculation(ProjectEvent.CalculateRiskMetrics event) {
        var result = projectService.updateRiskMetrics(event.projectName());
        if (result instanceof Result.Failure<?> failure) {
            saveToDLQ(event, failure.error());
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
