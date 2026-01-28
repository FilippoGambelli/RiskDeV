package it.unipi.riskDeV.async.events;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import it.unipi.riskDeV.DTO.project.InstalledPackageDTO;

import java.util.List;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = ProjectEvent.ProjectCreated.class, name = "ProjectCreated"),
    @JsonSubTypes.Type(value = ProjectEvent.ProjectDeleted.class, name = "ProjectDeleted"),
    @JsonSubTypes.Type(value = ProjectEvent.ProjectPackagesUpdated.class, name = "ProjectPackagesUpdated"),
    @JsonSubTypes.Type(value = ProjectEvent.CollaboratorAdded.class, name = "CollaboratorAdded"),
    @JsonSubTypes.Type(value = ProjectEvent.CollaboratorRemoved.class, name = "CollaboratorRemoved"),
    @JsonSubTypes.Type(value = ProjectEvent.CalculateRiskMetrics.class, name = "CalculateRiskMetricsEvent")
})
public sealed interface ProjectEvent {
    String projectName(); 

    record ProjectCreated(String projectName, String adminUsername, List<InstalledPackageDTO> packageIds) implements ProjectEvent {}
    record ProjectDeleted(String projectName, List<String> involvedCollaborators) implements ProjectEvent {}
    record ProjectPackagesUpdated(String projectName, List<InstalledPackageDTO> packageIds) implements ProjectEvent {}
    record CollaboratorAdded(String projectName, String collaboratorUsername) implements ProjectEvent {}
    record CollaboratorRemoved(String projectName, String collaboratorUsername) implements ProjectEvent {}
    record CalculateRiskMetrics(String projectName, List<InstalledPackageDTO> dtos) implements ProjectEvent {}
}