package it.unipi.riskDeV.async.scheduler;

import it.unipi.riskDeV.async.handlers.EventHandler;
import it.unipi.riskDeV.model.documentDB.FailedEvent;
import it.unipi.riskDeV.repository.documentDB.FailedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DlqRetryScheduler {

    private final FailedEventRepository failedEventRepository;
    private final List<EventHandler> eventHandlers; 
    private static final int BATCH_SIZE = 50;

    @Scheduled(fixedDelayString = "${app.scheduler.retry.delay:300000}")
    public void retryFailedEvents() {
        
        Pageable limit = PageRequest.of(0, BATCH_SIZE);
        List<FailedEvent> events = failedEventRepository.findByResolvedAtIsNullAndRetryCountLessThan(5, limit);

        if (events.isEmpty()) return;

        log.info("Processing batch of {} failed events.", events.size());

        for (FailedEvent failedEvent : events) {
            processSingleEvent(failedEvent);
        }
    }

    // Strategy pattern
    private void processSingleEvent(FailedEvent failedEvent) {
        try {
            EventHandler handler = eventHandlers.stream()
                .filter(h -> h.canHandle(failedEvent.getEventType()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No handler for type: " + failedEvent.getEventType()));

            // logic execution
            handler.handle(failedEvent.getPayloadJson());

            markAsResolved(failedEvent);

        } catch (Exception e) {
            markAsFailedAgain(failedEvent, e);
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
