package it.unipi.riskDeV.async.listeners;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.unipi.riskDeV.async.DocumentService;
import it.unipi.riskDeV.async.GraphService;
import it.unipi.riskDeV.async.events.PackageEvent;
import it.unipi.riskDeV.async.events.ProjectEvent;
import it.unipi.riskDeV.async.events.VulnerabilityEvent;
import it.unipi.riskDeV.model.documentDB.FailedEvent;
import it.unipi.riskDeV.repository.documentDB.FailedEventRepository;
import it.unipi.riskDeV.util.Helper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class GraphSyncListener {

    private final GraphService graphService;
    private final DocumentService documentService;
    private final FailedEventRepository failedEventRepository;
    private final ObjectMapper objectMapper;
    private final Helper helper;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleProjectEvent(ProjectEvent event) {
        try {
            log.debug("Neo4j Sync: Processing project event for {}", event.projectName());

            switch (event) {
                case ProjectEvent.ProjectCreated c -> 
                    graphService.createProjectStructure(c.projectName(), c.adminUsername(), c.packageIds());
                
                case ProjectEvent.ProjectDeleted d -> 
                    graphService.deleteProjectNode(d.projectName());
                
                case ProjectEvent.ProjectPackagesUpdated p -> 
                    graphService.syncProjectPackages(p.projectName(), p.packageIds());
                
                case ProjectEvent.CollaboratorAdded a -> 
                    graphService.addCollaborator(a.projectName(), a.collaboratorUsername());
                
                case ProjectEvent.CollaboratorRemoved r -> 
                    graphService.removeCollaborator(r.projectName(), r.collaboratorUsername());

                case ProjectEvent.CalculateRiskMetrics m -> {}
            }
        } catch (Exception e) {
            log.error("Neo4j Sync Failed: Error processing {} for project {}", event.getClass().getSimpleName(), event.projectName(), e);
            saveToDLQ(event, e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleVulnerabilityEvent(VulnerabilityEvent event) {
        try {
            log.debug("Neo4j Sync: Processing vulnerability event for CVE {}", event.cveId());
            
            switch (event) {
                case VulnerabilityEvent.VulnerabilityCreated c -> 
                    graphService.addVulnerability(c.cveId(), c.description(), c.baseScore());
                
                case VulnerabilityEvent.VulnerabilityUpdated u -> 
                    graphService.updateVulnerability(u.cveId(), u.description(), u.baseScore());
                
                case VulnerabilityEvent.VulnerabilityDeleted d -> 
                    graphService.deleteVulnerability(d.cveId());
            }
        } catch (Exception e) {
            log.error("Neo4j Sync Failed: Vulnerability event processing error for {}", event.cveId(), e);
            saveToDLQ(event, e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handlePackageEvent(PackageEvent event) {
        try {
            log.debug("Neo4j Sync: Processing package event for {}", event.packageName());

            switch (event) {
                case PackageEvent.VersionRelease v -> {
                    Double risk_score = helper.getMaxBaseScore(v.publishedVersionDTO().getVulnerabilities());
                    documentService.updateRiskScore(v.publishedVersionDTO().getPackageName(), v.publishedVersionDTO().getVersion(), risk_score);
                    graphService.addPackage(v.publishedVersionDTO(), risk_score);
                }
                
                case PackageEvent.UpdateDocumentation d -> 
                    graphService.updatePackageDocumentation(d.packageName(), d.documentationURL());
                
                case PackageEvent.UpdatePackageVersion u -> 
                    graphService.updatePackageVersion(u.packageName(), u.version(), u.dependecies(), u.vulnerabilities());
                
                case PackageEvent.DeletePackageVersion del -> 
                    graphService.deletePackageVersion(del.packageName(), del.version());
            }
        } catch (Exception e) {
            log.error("Neo4j Sync Failed: Error processing {} for package {}", event.getClass().getSimpleName(), event.packageName(), e);
            saveToDLQ(event, e);
        }
    }

    private void saveToDLQ(Object event, Exception e) {
        try {
            FailedEvent dlq = new FailedEvent();
            dlq.setEventType(event.getClass().getSimpleName());
            dlq.setPayloadJson(objectMapper.writeValueAsString(event));
            dlq.setExceptionMessage(e.getMessage());
            dlq.setFailedAt(Instant.now());
            dlq.setRetryCount(0);

            failedEventRepository.save(dlq);
        } catch (Exception jsonEx) {
            log.error("CRITICAL: Failed to serialize event for DLQ", jsonEx);
        }
    }
}