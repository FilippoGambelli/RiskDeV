package it.unipi.riskDeV.async.handlers.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.unipi.riskDeV.async.events.UserEvent;
import it.unipi.riskDeV.async.handlers.EventHandler;
import it.unipi.riskDeV.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserEventHandler implements EventHandler {

    private final ProjectService projectService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean canHandle(String eventType) {
        return eventType.startsWith("User");
    }

    @Override
    public void handle(String payloadJson) {
        UserEvent event; 
        try {
            event = objectMapper.readValue(payloadJson, UserEvent.class);
        } catch(Exception e) {
            throw new RuntimeException("JSON Deserialization failed", e);
        }
        
        log.debug("DLQ Retry: User event for {}", event.username());
        
        switch (event) {           
            case UserEvent.UserUpdated u -> projectService.changeCollaboratorDataInProjects(
                    u.projectNames(), u.username(), u.newUsername(), u.newEmail()
            );
                
            case UserEvent.UserDeleted d -> projectService.removeCollaboratorFromProjects(
                    d.projectNames(), d.username()
            );
        }
    }
}