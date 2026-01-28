package it.unipi.riskDeV.async.handlers.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.unipi.riskDeV.async.GraphService;
import it.unipi.riskDeV.async.events.ProjectEvent;
import it.unipi.riskDeV.async.handlers.EventHandler;
import it.unipi.riskDeV.results.DomainError;
import it.unipi.riskDeV.results.Result;
import it.unipi.riskDeV.service.ProjectService;
import it.unipi.riskDeV.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectEventHandler implements EventHandler {

    private final UserService userService;
    private final GraphService graphService;
    private final ProjectService projectService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean canHandle(String eventType) {
        return eventType.startsWith("Project") || eventType.contains("Collaborator");
    }

    @Override
    public Result<Void> handle(String payloadJson) {
        ProjectEvent event;
        try {
            event = objectMapper.readValue(payloadJson, ProjectEvent.class);    
        } catch (Exception e) {
            return new Result.Failure<>(new DomainError.SystemError());
        }
        
        
        log.debug("DLQ Retry: Handling project event for {}", event.projectName());

        return switch (event) {
            case ProjectEvent.ProjectCreated c -> 
                graphService.createProjectStructure(c.projectName(), c.adminUsername(), c.packageIds());
            
            case ProjectEvent.ProjectDeleted d -> 
                graphService.deleteProjectNode(d.projectName());
            
            case ProjectEvent.ProjectPackagesUpdated p -> 
                graphService.syncProjectPackages(p.projectName(), p.packageIds());
            
            case ProjectEvent.CollaboratorAdded a -> 
                userService.addProjectToUser(a.collaboratorUsername(),a.projectName());
            
            case ProjectEvent.CollaboratorRemoved r -> 
                userService.removeProjectFromUser(r.collaboratorUsername(),r.projectName());

            case ProjectEvent.CalculateRiskMetrics m -> 
                projectService.updateRiskMetrics(m.projectName());
        };
    }
}