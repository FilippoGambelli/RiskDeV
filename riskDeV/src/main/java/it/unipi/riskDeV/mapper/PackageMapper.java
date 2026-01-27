package it.unipi.riskDeV.mapper;

import it.unipi.riskDeV.DTO.packageVersion.*;
import it.unipi.riskDeV.model.documentDB.*;
import it.unipi.riskDeV.util.DependencyParser;
import it.unipi.riskDeV.util.VersionParser;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Mapper(
    componentModel = "spring",
    imports = { Instant.class, ArrayList.class, java.util.Objects.class },
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public abstract class PackageMapper {

    @Autowired
    protected VersionParser versionParser;

    // --- 1. MAPPING: CREAZIONE (AddPackageVersionDTO -> PackageVersion) ---
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "riskScore", constant = "0.0") 
    @Mapping(target = "uploadTime", expression = "java(Instant.now().toString())")
    @Mapping(target = "versionArray", source = "version", qualifiedByName = "toVersionArray")
    // MapStruct userà mapStringToConstraints per ogni elemento della List<String>
    @Mapping(target = "dependencies", source = "dependencies")
    public abstract PackageVersion toEntity(AddPackageVersionDTO dto);

    // --- 2. MAPPING: AGGIORNAMENTO (UpdatePackageVersionDTO -> @MappingTarget PackageVersion) ---
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "packageName", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "versionArray", ignore = true)
    // MapStruct userà toConstraints per ogni elemento della List<ConstraintsDTO>
    @Mapping(target = "dependencies", source = "dependencies")
    public abstract void updateEntityFromDto(UpdatePackageVersionDTO dto, @MappingTarget PackageVersion entity);

    // --- 3. MAPPING: LETTURA (PackageVersion -> PackageVersionDTO) ---
    // MapStruct userà mapConstraintsToString per ogni elemento della List<Constraints>
    @Mapping(target = "dependencies", source = "dependencies")
    public abstract PackageVersionDTO toDto(PackageVersion model);

    // --- 4. MAPPING: EVENTI (PackageVersion -> PublishedVersionDTO) ---
    public abstract PublishedVersionDTO toPublishedDto(PackageVersion model);

    // --- 5. MAPPING OGGETTI ANNIDATI (Child Entities) ---
    
    // Vulnerabilità
    public abstract EmbeddedVulnerability toVulnEntity(EmbeddedVulnerabilityDTO dto);
    public abstract EmbeddedVulnerabilityDTO toVulnDto(EmbeddedVulnerability model);

    // Vincoli (Constraints)
    public abstract Constraints toConstraints(ConstraintsDTO dto);
    public abstract ConstraintsDTO toConstraintsDto(Constraints model);

    // --- 6. LOGICA DI TRASFORMAZIONE CUSTOM ---

    /**
     * Converte la stringa della versione in array per il sorting (MongoDB).
     */
    @Named("toVersionArray")
    protected List<Integer> toVersionArray(String version) {
        return (version != null) ? versionParser.generateVersionArray(version) : new ArrayList<>();
    }

    /**
     * Converte stringhe raw (es. "urllib3>=1.2") in oggetti Constraints (Create flow).
     */
    protected Constraints mapStringToConstraints(String dep) {
        return (dep != null) ? DependencyParser.parseFullString(dep) : null;
    }

    /**
     * Converte oggetti Constraints nella loro rappresentazione testuale (Read flow).
     */
    protected String mapConstraintsToString(Constraints constraints) {
        return (constraints != null) ? constraints.getFull() : null;
    }

    /**
     * Utility per gestire i campi Optional del DTO durante gli update.
     */
    protected <T> T mapOptional(Optional<T> optional) {
        if (optional == null) return null;
        return optional.orElse(null);
    }
}