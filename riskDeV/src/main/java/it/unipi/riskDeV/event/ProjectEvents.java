package it.unipi.riskDeV.event;

import java.util.List;

public class ProjectEvents {

    // Create projcet node, admin and owns relationship
    // "projectName projectVersion" for packageIds
    public record ProjectCreatedEvent(String projectName, String adminId, List<String> packageIds) {}
    
    // Delete project node and all relationships
    public record ProjectDeletedEvent(String projectName) {}
    
    // Delete old uses relationship, update them with the new ones
    public record ProjectPackagesUpdatedEvent(String projectName, List<String> packageIds) {}

    // New WORKS_ON relationship
    public record CollaboratorAddedEvent(String projectName, String userId) {}
    
    // Collaboratore -> DELETE relazione WORKS_ON
    public record CollaboratorRemovedEvent(String projectName, String userId) {}
}