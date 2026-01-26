package it.unipi.riskDeV.event;
import it.unipi.riskDeV.DTO.packageVersion.ConstraintsDTO;
import it.unipi.riskDeV.DTO.packageVersion.EmbeddedVulnerabilityDTO;
import it.unipi.riskDeV.DTO.packageVersion.PublishedVersionDTO;

import java.util.List;

public class PackageEvent {
    public record VersionReleaseEvent(PublishedVersionDTO publishedVersionDTO){}
    public record UpdateDocumentationEvent(String packageName, String documentationURL){}
    public record UpdatePackageVersionEvent(String packageName, String version, List<ConstraintsDTO> dependecies, List<EmbeddedVulnerabilityDTO> vulnerabilities){}
    public record DeletePackageVersionEvent(String packageName, String version){}
}
