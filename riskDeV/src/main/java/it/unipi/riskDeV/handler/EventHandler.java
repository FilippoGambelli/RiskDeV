package it.unipi.riskDeV.handler;

public interface EventHandler {
    boolean canHandle(String eventType);
    void handle(String payloadJson) throws Exception;
}
