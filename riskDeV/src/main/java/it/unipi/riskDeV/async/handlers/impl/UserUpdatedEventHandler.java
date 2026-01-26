package it.unipi.riskDeV.async.handlers.impl;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.unipi.riskDeV.async.GraphService;
import it.unipi.riskDeV.async.events.UserEvents;
import it.unipi.riskDeV.async.handlers.EventHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserUpdatedEventHandler implements EventHandler {

    private final GraphService graphService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean canHandle(String eventType) {
        return "User.UserUpdatedEvent".equals(eventType);
    }

    @Override
    public void handle(String payloadJson) throws Exception {
        var event = objectMapper.readValue(payloadJson, UserEvents.UserUpdatedEvent.class);
        graphService.updateUsername(event.oldUsername(), event.newUsername());
    }
}