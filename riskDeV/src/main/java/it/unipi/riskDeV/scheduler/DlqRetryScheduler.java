package it.unipi.riskDeV.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.unipi.riskDeV.event.UserEvent;
import it.unipi.riskDeV.model.FailedEvent;
import it.unipi.riskDeV.repository.FailedEventRepository;
import it.unipi.riskDeV.service.GraphService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DlqRetryScheduler {

    private final FailedEventRepository failedEventRepository;
    private final GraphService graphService;
    private final ObjectMapper objectMapper;

    // Execute every 5 minutes
    @Scheduled(fixedDelay = 300_000) 
    public void retryFailedEvents() {
        
        // Seach for failed events that are not resolved and have retryCount < maxRetries
        List<FailedEvent> events = failedEventRepository.findByResolvedAtIsNullAndRetryCountLessThan(5);

        if (events.isEmpty()) return;

        log.info("Found {} events to retry...", events.size());

        // For every failed event try to reprocess
        for (FailedEvent failedEvent : events) {
            try {
                if ("UserDeletedEvent".equals(failedEvent.getEventType())) {
                    
                    UserEvent.UserDeletedEvent originalEvent = objectMapper.readValue(
                        failedEvent.getPayloadJson(), UserEvent.UserDeletedEvent.class
                    );
                    
                    // Retry the operation
                    graphService.deleteUserNode(originalEvent.username());
                }
                
                /*******************************************/
                /* WE COULD ADD MORE EVENT TYPES TO MANAGE */
                /*******************************************/ 

                // Remove event from DLQ
                markAsResolved(failedEvent);

            } catch (Exception e) {
                // Update retry count and last retry timestamp
                markAsFailedAgain(failedEvent, e);
            }
        }
    }

    private void markAsResolved(FailedEvent event) {
        event.setResolvedAt(Instant.now());
        event.setExceptionMessage("Resolved by Scheduler");
        failedEventRepository.save(event);
        log.info("Event {} healed successfully.", event.getId());
    }

    private void markAsFailedAgain(FailedEvent event, Exception e) {
        event.setRetryCount(event.getRetryCount() + 1);
        event.setLastRetryAt(Instant.now());
        event.setExceptionMessage(e.getMessage());
        failedEventRepository.save(event);
        log.error("Retry failed for event {}. Count: {}", event.getId(), event.getRetryCount());
    }
}
