package it.unipi.riskDeV.async.handlers.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.unipi.riskDeV.async.GraphService;
import it.unipi.riskDeV.async.events.ProjectEvent;
import it.unipi.riskDeV.async.handlers.EventHandler;
import it.unipi.riskDeV.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectEventHandler implements EventHandler {

    private final GraphService graphService;
    private final ProjectService projectService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean canHandle(String eventType) {
        return eventType.startsWith("Project") || eventType.contains("Collaborator");
    }

    @Override
    public void handle(String payloadJson) throws Exception {
        ProjectEvent event = objectMapper.readValue(payloadJson, ProjectEvent.class);
        
        log.debug("DLQ Retry: Handling project event for {}", event.projectName());

        switch (event) {
            case ProjectEvent.ProjectCreated c -> 
                graphService.createProjectStructure(c.projectName(), c.adminUsername(), c.packageIds());
            
            case ProjectEvent.ProjectDeleted d -> 
                graphService.deleteProjectNode(d.projectName());
            
            case ProjectEvent.ProjectPackagesUpdated p -> 
                graphService.syncProjectPackages(p.projectName(), p.packageIds());
            
            case ProjectEvent.CollaboratorAdded a -> 
                graphService.addCollaborator(a.projectName(), a.collaboratorUsername());
            
            case ProjectEvent.CollaboratorRemoved r -> 
                graphService.removeCollaborator(r.projectName(), r.collaboratorUsername());

            case ProjectEvent.CalculateRiskMetrics m -> 
                projectService.updateRiskMetrics(m.projectName());
        }
    }
}