package it.unipi.riskDeV.DTO.project;

import it.unipi.riskDeV.DTO.CollaboratorDTO;
import it.unipi.riskDeV.DTO.InstalledPackageDTO;
import it.unipi.riskDeV.model.Project;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class ProjectDTO {

    private String name;
    private String description;
    private Instant lastUpdate;
    private String pythonVersion;
    private CollaboratorDTO admin;
    private List<InstalledPackageDTO> packages;
    private List<CollaboratorDTO> collaborators;

    // Costruttore principale
    public ProjectDTO(String name, String description, Instant lastUpdate,
                      String pythonVersion, CollaboratorDTO admin,
                      List<InstalledPackageDTO> packages,
                      List<CollaboratorDTO> collaborators) {
        this.name = name;
        this.description = description;
        this.lastUpdate = lastUpdate;
        this.pythonVersion = pythonVersion;
        this.admin = admin;
        this.packages = packages;
        this.collaborators = collaborators;
    }

    // Costruttore di conversione da Project
    public ProjectDTO(Project project) {
        this.name = project.getName();
        this.description = project.getDescription();
        this.lastUpdate = project.getLastUpdate();
        this.pythonVersion = project.getPythonVersion();
        this.admin = project.getAdmin() != null ? new CollaboratorDTO(project.getAdmin()) : null;

        // mapping delle liste
        this.packages = new ArrayList<>();
        if (project.getPackages() != null) {
            for (Project.ProjectPackage p : project.getPackages()) {
                this.packages.add(new InstalledPackageDTO(p));
            }
        }

        this.collaborators = new ArrayList<>();
        if (project.getCollaborators() != null) {
            for (Project.Collaborator c : project.getCollaborators()) {
                this.collaborators.add(new CollaboratorDTO(c));
            }
        }
    }

    // Getters e Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Instant getLastUpdate() { return lastUpdate; }
    public void setLastUpdate(Instant lastUpdate) { this.lastUpdate = lastUpdate; }
    public String getPythonVersion() { return pythonVersion; }
    public void setPythonVersion(String pythonVersion) { this.pythonVersion = pythonVersion; }
    public CollaboratorDTO getAdmin() { return admin; }
    public void setAdmin(CollaboratorDTO admin) { this.admin = admin; }
    public List<InstalledPackageDTO> getPackages() { return packages; }
    public void setPackages(List<InstalledPackageDTO> packages) { this.packages = packages; }
    public List<CollaboratorDTO> getCollaborators() { return collaborators; }
    public void setCollaborators(List<CollaboratorDTO> collaborators) { this.collaborators = collaborators; }
}