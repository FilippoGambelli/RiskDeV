package it.unipi.riskDeV.async.handlers.impl;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.unipi.riskDeV.async.DocumentService;
import it.unipi.riskDeV.async.GraphService;
import it.unipi.riskDeV.async.events.PackageEvent;
import it.unipi.riskDeV.async.handlers.EventHandler;
import it.unipi.riskDeV.results.DomainError;
import it.unipi.riskDeV.results.Result;
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
    public Result<Void> handle(String payloadJson) {
        PackageEvent event;
        try {
            event = objectMapper.readValue(payloadJson, PackageEvent.class);
        } catch (Exception e) {
            return new Result.Failure<>(new DomainError.SystemError(e));
        }
        
        log.debug("DLQ Retry: Synchronizing package graph for {}", event.packageName());

        return switch (event) {
            case PackageEvent.VersionRelease v -> {
                Double risk_score = helper.getMaxBaseScore(v.publishedVersionDTO().getVulnerabilities());
                var updateResult = documentService.updateRiskScore(v.publishedVersionDTO().getPackageName(), v.publishedVersionDTO().getVersion(), risk_score);
                var addResult = graphService.addPackage(v.publishedVersionDTO(), risk_score);
                if (updateResult instanceof Result.Failure<?> || addResult instanceof Result.Failure<?>) {
                    yield new Result.Failure<>(new DomainError.SystemError("Package add in graph failed"));
                } else {
                    yield new Result.Success<>(null);
                }
            }
            
            case PackageEvent.UpdateDocumentation d -> 
                graphService.updatePackageDocumentation(d.packageName(), d.documentationURL());
            
            case PackageEvent.UpdatePackageVersion u -> 
                graphService.updatePackageVersion(u.packageName(), u.version(), u.dependecies(), u.vulnerabilities());
            
            case PackageEvent.DeletePackageVersion del -> 
                graphService.deletePackageVersion(del.packageName(), del.version());
        };
    }
}