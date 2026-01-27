package it.unipi.riskDeV.async.events;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import it.unipi.riskDeV.DTO.packageVersion.ConstraintsDTO;
import it.unipi.riskDeV.DTO.packageVersion.EmbeddedVulnerabilityDTO;
import it.unipi.riskDeV.DTO.packageVersion.PublishedVersionDTO;

import java.util.List;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = PackageEvent.VersionRelease.class, name = "VersionReleaseEvent"),
    @JsonSubTypes.Type(value = PackageEvent.UpdateDocumentation.class, name = "UpdateDocumentationEvent"),
    @JsonSubTypes.Type(value = PackageEvent.UpdatePackageVersion.class, name = "UpdatePackageVersionEvent"),
    @JsonSubTypes.Type(value = PackageEvent.DeletePackageVersion.class, name = "DeletePackageVersionEvent")
})
public sealed interface PackageEvent {
    String packageName();

    record VersionRelease(PublishedVersionDTO publishedVersionDTO) implements PackageEvent {
        @Override public String packageName() { return publishedVersionDTO.getPackageName(); }
    }

    record UpdateDocumentation(String packageName, String documentationURL) implements PackageEvent {}

    record UpdatePackageVersion(String packageName, String version,List<ConstraintsDTO> dependecies, List<EmbeddedVulnerabilityDTO> vulnerabilities) implements PackageEvent {}

    record DeletePackageVersion(String packageName, String version) implements PackageEvent {}
}