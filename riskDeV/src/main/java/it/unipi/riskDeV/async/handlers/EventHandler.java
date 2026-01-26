package it.unipi.riskDeV.async.handlers;

public interface EventHandler {
    boolean canHandle(String eventType);
    void handle(String payloadJson) throws Exception;
}
