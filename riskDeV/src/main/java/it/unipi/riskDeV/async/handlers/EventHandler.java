package it.unipi.riskDeV.async.handlers;

import it.unipi.riskDeV.results.Result;

public interface EventHandler {
    boolean canHandle(String eventType);
    Result<Void> handle(String payloadJson);
}
