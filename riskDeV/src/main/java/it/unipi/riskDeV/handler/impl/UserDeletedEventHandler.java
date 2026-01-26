package it.unipi.riskDeV.handler.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.unipi.riskDeV.event.UserEvent;
import it.unipi.riskDeV.handler.EventHandler;
import it.unipi.riskDeV.service.GraphService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserDeletedEventHandler implements EventHandler {

    private final GraphService graphService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean canHandle(String eventType) {
        return "User.UserDeletedEvent".equals(eventType);
    }

    @Override
    public void handle(String payloadJson) throws Exception {
        var event = objectMapper.readValue(payloadJson, UserEvent.UserDeletedEvent.class);
        graphService.deleteUserNode(event.username());
    }
}