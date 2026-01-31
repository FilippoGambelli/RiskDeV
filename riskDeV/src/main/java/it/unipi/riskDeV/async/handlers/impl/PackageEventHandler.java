package it.unipi.riskDeV.async.handlers.impl;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.unipi.riskDeV.async.DocumentService;
import it.unipi.riskDeV.async.GraphService;
import it.unipi.riskDeV.async.events.PackageEvent;
import it.unipi.riskDeV.async.handlers.EventHandler;
import it.unipi.riskDeV.util.Helper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PackageEventHandler implements EventHandler {

    private final GraphService graphService;
    private final DocumentService documentService;
    private final ObjectMapper objectMapper;
    private final Helper helper;

    @Override
    public boolean canHandle(String eventType) {
        return eventType.contains("Package") || eventType.contains("Version") || eventType.contains("Documentation");
    }

    @Override
    public void handle(String payloadJson) {
        PackageEvent event;
        try {
            event = objectMapper.readValue(payloadJson, PackageEvent.class);
        } catch (Exception e) {
            throw new RuntimeException("JSON Deserialization failed", e);
        }
        
        log.debug("DLQ Retry: Package event for {}", event.packageName());

        switch (event) {
            case PackageEvent.VersionRelease v -> {
                Double riskScore = helper.getMaxBaseScore(v.publishedVersionDTO().getVulnerabilities());
                
                documentService.updateRiskScore(v.publishedVersionDTO().getPackageName(), v.publishedVersionDTO().getVersion(), riskScore);
                graphService.addPackage(v.publishedVersionDTO(), riskScore);
            }
            
            case PackageEvent.UpdateDocumentation d -> 
                graphService.updatePackageDocumentation(d.packageName(), d.documentationURL());
            
            case PackageEvent.UpdatePackageVersion u -> 
                graphService.updatePackageVersion(u.packageName(), u.version(), u.dependecies(), u.vulnerabilities());
            
            case PackageEvent.DeletePackageVersion del -> 
                graphService.deletePackageVersion(del.packageName(), del.version());
        }
    }
}