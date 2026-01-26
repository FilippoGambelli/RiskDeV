package it.unipi.riskDeV.repository.documentDB;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import it.unipi.riskDeV.model.documentDB.FailedEvent;

import java.util.List;

public interface FailedEventRepository extends MongoRepository<FailedEvent, String> {
    
    // Retrieve failed events that are not resolved and have retry count less than maxRetries
    List<FailedEvent> findByResolvedAtIsNullAndRetryCountLessThan(int maxRetries, Pageable pageable);
}