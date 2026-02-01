package it.unipi.riskDeV.async.listeners;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import it.unipi.riskDeV.async.FailedEventService;
import it.unipi.riskDeV.async.events.ProjectEvent;
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
    private final FailedEventService failedEventService;

    @Async
    @EventListener
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
                    projectService.updateRiskMetrics(event.projectName());

                case ProjectEvent.ProjectPackagesUpdated u -> {}
            }
        } catch (Exception e) {
            log.error("Unexpected error in ProjectListener", e);
            failedEventService.saveError(event, "Unexpected System Error: " + e.getMessage());
        }
    }

    private void syncUserAction(String username, String projectName, boolean isAdd, ProjectEvent originalEvent) {
        Result<?> result = isAdd 
            ? userService.addProjectToUser(username, projectName) 
            : userService.removeProjectFromUser(username, projectName);
            
        if (result instanceof Result.Failure<?> failure) {
            failedEventService.saveError(originalEvent, failure.error().message());
        }
    }

    private void handleProjectDeletionCleanup(ProjectEvent.ProjectDeleted event) {
        if (event.involvedCollaborators() == null || event.involvedCollaborators().isEmpty()) return;

        for (String username : event.involvedCollaborators()) {
            Result<?> result = userService.removeProjectFromUser(username, event.projectName());
            
            if (result instanceof Result.Failure<?> failure) {
                var failedContext = new ProjectEvent.CollaboratorRemoved(event.projectName(), username);
                failedEventService.saveError(failedContext, "Cleanup failed during deletion: " + failure.error().message());
            }
        }
    }
}
