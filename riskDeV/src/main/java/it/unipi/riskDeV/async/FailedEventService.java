package it.unipi.riskDeV.async;

import java.time.Instant;

import org.springframework.stereotype.Service;


import com.fasterxml.jackson.databind.ObjectMapper;

import it.unipi.riskDeV.model.documentDB.FailedEvent;
import it.unipi.riskDeV.repository.documentDB.FailedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class FailedEventService {

    private final FailedEventRepository failedEventRepository;
    private final ObjectMapper objectMapper;

    // Open another transaction, this must be indipendent from the precedent (it could do rollback)
    public void saveError(Object event, String errorMessage) {
        try {
            FailedEvent dlq = new FailedEvent();
            dlq.setEventType(event.getClass().getSimpleName());
            dlq.setExceptionMessage(errorMessage);
            dlq.setFailedAt(Instant.now());
            dlq.setRetryCount(0); 
            
            try {
                String jsonPayload = objectMapper.writeValueAsString(event);
                dlq.setPayloadJson(jsonPayload);
            } catch (Exception jsonEx) {
                log.error("Failed to serialize event payload to JSON", jsonEx);
                dlq.setPayloadJson("Serialization Failed: " + event.toString());
            }

            failedEventRepository.save(dlq);
            
            log.warn("[DLQ] Event {} saved to DLQ. Reason: {}", event.getClass().getSimpleName(), errorMessage);

        } catch (Exception e) {
            log.error("CRITICAL: Failed to save event to DLQ. Original Error: {}. DLQ Error: {}", errorMessage, e.getMessage(), e);
        }
    }
}