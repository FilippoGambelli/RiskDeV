package it.unipi.riskDeV.event;
import it.unipi.riskDeV.model.PackageVersion.Constraints;
import it.unipi.riskDeV.model.PackageVersion.EmbeddedVulnerability;

import java.util.List;

import it.unipi.riskDeV.DTO.PublishedVersionDTO;

public class PackageEvent {
    public record VersionReleaseEvent(PublishedVersionDTO publishedVersionDTO){}
    public record UpdateDocumentationEvent(String packageName, String documentationURL){}
    public record UpdatePackageVersionEvent(String packageName, String version, List<Constraints> dependecies, List<EmbeddedVulnerability> vulnerabilities){}
    public record DeletePackageVersionEvent(String packageName, String version){}
}
