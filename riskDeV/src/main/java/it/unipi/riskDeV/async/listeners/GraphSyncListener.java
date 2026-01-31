package it.unipi.riskDeV.async.listeners;

import it.unipi.riskDeV.async.DocumentService;
import it.unipi.riskDeV.async.FailedEventService;
import it.unipi.riskDeV.async.GraphService;
import it.unipi.riskDeV.async.events.PackageEvent;
import it.unipi.riskDeV.async.events.ProjectEvent;
import it.unipi.riskDeV.async.events.VulnerabilityEvent;
import it.unipi.riskDeV.util.Helper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class GraphSyncListener {

    private final GraphService graphService;
    private final DocumentService documentService;
    private final FailedEventService failedEventService; 
    private final Helper helper;

    @Async
    public void handleProjectEvent(ProjectEvent event) {
        log.info("[Graph Listener] Processing project event: {}", event.projectName());

        try {
            switch (event) {
                case ProjectEvent.ProjectCreated c -> 
                    graphService.createProjectStructure(c.projectName(), c.adminUsername(), c.projectPackages());
                
                case ProjectEvent.ProjectDeleted d -> 
                    graphService.deleteProjectNode(d.projectName());
                
                case ProjectEvent.ProjectPackagesUpdated p -> 
                    graphService.syncProjectPackages(p.projectName(), p.projectPackages());
                
                default -> {}
            }
        } catch (Exception e) {
            failedEventService.saveError(event, "Neo4j Project Sync Error: " + e.getMessage());
        }
    }

    @Async
    public void handleVulnerabilityEvent(VulnerabilityEvent event) {
        log.debug("[Graph Listener] Processing vulnerability event: {}", event.cveId());

        try {
            switch (event) {
                case VulnerabilityEvent.VulnerabilityCreated c -> 
                    graphService.addVulnerability(c.cveId(), c.description(), c.baseScore());
                
                case VulnerabilityEvent.VulnerabilityUpdated u -> 
                    graphService.updateVulnerability(u.cveId(), u.description(), u.baseScore());
                
                case VulnerabilityEvent.VulnerabilityDeleted d -> 
                    graphService.deleteVulnerability(d.cveId());
            }
        } catch (Exception e) {
            failedEventService.saveError(event, "Neo4j Vuln Sync Error: " + e.getMessage());
        }
    }

    @Async
    public void handlePackageEvent(PackageEvent event) {
        log.debug("[Graph Listener] Processing package event: {}", event.packageName());

        try {
            switch (event) {
                case PackageEvent.VersionRelease v -> 
                    handleVersionRelease(v);
                
                case PackageEvent.UpdateDocumentation d -> 
                    graphService.updatePackageDocumentation(d.packageName(), d.documentationURL());
                
                case PackageEvent.UpdatePackageVersion u -> 
                    graphService.updatePackageVersion(u.packageName(), u.version(), u.dependecies(), u.vulnerabilities());
                
                case PackageEvent.DeletePackageVersion del -> 
                    graphService.deletePackageVersion(del.packageName(), del.version());
            }
        } catch (Exception e) {
            failedEventService.saveError(event, "Neo4j Package Sync Error: " + e.getMessage());
        }
    }

    private void handleVersionRelease(PackageEvent.VersionRelease v) {
        Double riskScore = helper.getMaxBaseScore(v.publishedVersionDTO().getVulnerabilities());
        
        documentService.updateRiskScore(v.publishedVersionDTO().getPackageName(), v.publishedVersionDTO().getVersion(), riskScore);
        graphService.addPackage(v.publishedVersionDTO(), riskScore);
    }
}