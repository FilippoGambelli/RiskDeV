package it.unipi.riskDeV.async.handlers.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.unipi.riskDeV.async.GraphService;
import it.unipi.riskDeV.async.events.PackageEvent;
import it.unipi.riskDeV.async.handlers.EventHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PackageEventHandler implements EventHandler {

    private final GraphService graphService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean canHandle(String eventType) {
        return eventType.contains("Package") || eventType.contains("Version") || eventType.contains("Documentation");
    }

    @Override
    public void handle(String payloadJson) throws Exception {
        PackageEvent event = objectMapper.readValue(payloadJson, PackageEvent.class);
        
        log.debug("DLQ Retry: Synchronizing package graph for {}", event.packageName());

        switch (event) {
            case PackageEvent.VersionRelease v -> 
                graphService.addPackage(v.publishedVersionDTO());
            
            case PackageEvent.UpdateDocumentation d -> 
                graphService.updatePackageDocumentation(d.packageName(), d.documentationURL());
            
            case PackageEvent.UpdatePackageVersion u -> 
                graphService.updatePackageVersion(u.packageName(), u.version(), u.dependecies(), u.vulnerabilities());
            
            case PackageEvent.DeletePackageVersion del -> 
                graphService.deletePackageVersion(del.packageName(), del.version());
        }
    }
}