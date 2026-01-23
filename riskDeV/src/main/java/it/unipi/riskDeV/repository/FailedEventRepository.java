package it.unipi.riskDeV.repository;

import it.unipi.riskDeV.model.FailedEvent;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface FailedEventRepository extends MongoRepository<FailedEvent, String> {
    
    // Retrieve failed events that are not resolved and have retry count less than maxRetries
    List<FailedEvent> findByResolvedFalseAndRetryCountLessThan(int maxRetries);
}