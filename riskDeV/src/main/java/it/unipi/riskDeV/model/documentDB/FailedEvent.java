package it.unipi.riskDeV.model.documentDB;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Data
@Document(collection = "dlq")
public class FailedEvent {
    @Id
    private String id;
    
    private String eventType;  

    private String payloadJson;  

    private String exceptionMessage;

    private Instant failedAt;

    private Instant lastRetryAt;

    private int retryCount;        

    @Indexed(expireAfter = "7d") 
    private Instant resolvedAt;   
}
