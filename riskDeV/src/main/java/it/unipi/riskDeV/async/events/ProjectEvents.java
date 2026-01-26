package it.unipi.riskDeV.async.events;

import java.util.List;

import it.unipi.riskDeV.DTO.project.InstalledPackageDTO;

public class ProjectEvents {

    // Create projcet node, admin and owns relationship
    // "projectName projectVersion" for packageIds
    public record ProjectCreatedEvent(String projectName, String adminUsername, List<String> packageIds) {}
    
    // Delete project node and all relationships
    public record ProjectDeletedEvent(String projectName, List<String> involvedCollaborators) {}
    
    // Delete old uses relationship, update them with the new ones
    public record ProjectPackagesUpdatedEvent(String projectName, List<String> packageIds) {}

    // Recalculate risk metrics for the project
    public record CalculateRiskMetricsEvent(String projectName, List<InstalledPackageDTO> currentPackages) {}

    // New WORKS_ON relationship
    public record CollaboratorAddedEvent(String projectName, String collaboratorUsername) {}
    
    // Collaboratore -> DELETE relazione WORKS_ON
    public record CollaboratorRemovedEvent(String projectName, String collaboratorUsername) {}
}