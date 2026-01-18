package it.unipi.riskDeV.service;

import it.unipi.riskDeV.DTO.GeneralPackageDTO;
import it.unipi.riskDeV.DTO.PackageVersionDTO;
import it.unipi.riskDeV.common.DomainError;
import it.unipi.riskDeV.common.Result;
import it.unipi.riskDeV.model.PackageVersion;
import it.unipi.riskDeV.model.PackageVersion.EmbeddedVulnerability;
import it.unipi.riskDeV.repository.PackageVersionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import it.unipi.riskDeV.repository.PackageVersionGraphRepository;
import it.unipi.riskDeV.repository.PackageGraphRepository;
import it.unipi.riskDeV.model.neo4j.PackageVersionNode;
import it.unipi.riskDeV.model.neo4j.PackageNode;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.HashMap;
import java.time.Instant;



@Service
@RequiredArgsConstructor
@Slf4j
public class PackageService {

    private final PackageVersionRepository packageVersionRepository;
    private final PackageGraphRepository packageGraphRepository;
    private final PackageVersionGraphRepository packageVersionGraphRepository;
    private static final Pattern DEP_PATTERN = Pattern.compile("^([a-zA-Z0-9_\\-.]+)\\s*([<>=!~]+)?\\s*(.*)$");

    // Get a package by its name (returns metadata from the latest version)
    public Result<GeneralPackageDTO> getPackageByName(String packageName) {
        var pkgOpt = packageVersionRepository.findTopByPackageNameOrderByUploadTimeDesc(packageName);

        if (pkgOpt.isEmpty()) {
            return new Result.Failure<>(new DomainError.NotFound("Package " + packageName + " not found."));
        }

        return new Result.Success<>(new GeneralPackageDTO(pkgOpt.get()));
    }

    // Get information about a specific version of a package 
    public Result<PackageVersionDTO> getPackageByNameVersion(String packageName, String packageVersion) {
        var pkgVerOpt = packageVersionRepository.findByPackageNameAndVersion(packageName, packageVersion);
        if (pkgVerOpt.isEmpty()) {
            return new Result.Failure<>(new DomainError.NotFound("Version " + packageVersion + " of package " + packageName + " not found."));
        }
        
        return new Result.Success<>(new PackageVersionDTO(pkgVerOpt.get()));
    }

    // Get all the dependencies required by a package
    public Result<List<String>> getPackagesDependingOn(String packageName) {
        log.info("Searching for packages depending on: {}", packageName);

        // Checking if the package exists on neo4j
        if (!packageGraphRepository.existsById(packageName)) {
            return new Result.Failure<>(new DomainError.NotFound("Package " + packageName + " not found in the system."));
        }

        try {
            var dependents = packageVersionGraphRepository.findReverseDependencies(packageName);
            var ids = dependents.stream().map(PackageVersionNode::getId).toList();
            return new Result.Success<>(ids);
        } catch (Exception e) {
            return new Result.Failure<>(new DomainError.SystemError("Neo4j error fetching dependents", e));
        }
    }

    // Get all the dependencies of a specific package
    public Result<List<String>> getDirectDependencies(String packageName, String version) {
        // Id is defined as packageName + ' ' + version
        String versionId = packageName + " " + version;
        log.info("Searching for direct dependencies of: {}", versionId);

        // Checking if the node exists
        if (!packageVersionGraphRepository.existsById(versionId)) {
            return new Result.Failure<>(new DomainError.NotFound("Package version " + versionId + " not found in the graph."));
        }

        List<PackageVersionNode> dependencies = packageVersionGraphRepository.findDirectDependencies(versionId);
        var ids = dependencies.stream().map(PackageVersionNode::getId).toList();
        return new Result.Success<>(ids);
    }

    // Get versions without CVEs of a specific package
    public Result<List<PackageVersionDTO>> getSafeVersions(String packageName) {
        log.info("Searching for safe versions of package: {}", packageName);

        // Checking if the package exists
        if (!packageVersionGraphRepository.existsById(packageName)) {
            return new Result.Failure<>(new DomainError.NotFound("Package " + packageName + " not found in the system."));
        }

        List<PackageVersion> safeVersions = packageVersionRepository.findSafeVersions(packageName);

        // Mapping a DTO
        var versions = safeVersions.stream().map(PackageVersionDTO::new).toList();
        return new Result.Success<>(versions);
    }

    // Add a new package (only Neo4j)
    public Result<Void> addNewPackage(GeneralPackageDTO packageDTO) {
        String packageName = packageDTO.getPackageName();
        log.info("Registering new package structure for: {}", packageName);

        // Checks for validation on Neo4j
        if (packageGraphRepository.existsById(packageName)) {
            return new Result.Failure<>(new DomainError.AlreadyExists("Package " + packageName + " already exists."));
        }

        // Write on Neo4j
        try {
            PackageNode packageNode = new PackageNode();
            packageNode.setId(packageName);
            
            packageGraphRepository.save(packageNode);
            
            log.info("Successfully registered package node {}", packageName);

        } catch (Exception e) {
            return new Result.Failure<>(new DomainError.SystemError("Failed to create package node", e));
        }
        
        return new Result.Success<>(null);
    }

    // Add a new version of an existing package
    public Result<Void> addNewVersion(String packageName, PackageVersionDTO newVersionDTO) {
        String version = newVersionDTO.getVersion();
        log.info("Adding new version {} to package {}", version, packageName);

        // Checks for validation
        if (!packageGraphRepository.existsById(packageName)) {
            return new Result.Failure<>(new DomainError.NotFound("Package " + packageName + " not found (please register it first)."));
        }

        if (packageVersionRepository.existsByPackageNameAndVersion(packageName, version)) {
            return new Result.Failure<>(new DomainError.AlreadyExists("Version " + version + " already exists."));
        }

        // --- Write on MongoDB ---
        // Creation of the document
        PackageVersion versionDoc = new PackageVersion();
        versionDoc.setPackageName(packageName);
        versionDoc.setVersion(version);
        versionDoc.setUploadTime(Instant.now().toString());
        versionDoc.setRequiresPython(newVersionDTO.getRequiresPython());
        versionDoc.setDependencies(newVersionDTO.getDependencies());
        if (newVersionDTO.getVulnerabilities() != null) {
            versionDoc.setVulnerabilities(newVersionDTO.getVulnerabilities());
        } else {
            versionDoc.setVulnerabilities(new ArrayList<>());
        }
        versionDoc.setAuthor(newVersionDTO.getAuthor());
        versionDoc.setAuthorEmail(newVersionDTO.getAuthorEmail());
        versionDoc.setDescription(newVersionDTO.getDescription());
        versionDoc.setPackageURL(newVersionDTO.getPackageURL());
        versionDoc.setDocumentationURL(newVersionDTO.getDocumentationURL());

        // If we miss general metadata we can retrieve them from the latest version of that package (it's a fallback)
        if (versionDoc.getAuthor() == null || versionDoc.getDescription() == null) {
            var previousVersionOpt = packageVersionRepository.findTopByPackageNameOrderByUploadTimeDesc(packageName);
            if (previousVersionOpt.isPresent()) {
                PackageVersion prev = previousVersionOpt.get();
                if (versionDoc.getAuthor() == null) versionDoc.setAuthor(prev.getAuthor());
                if (versionDoc.getAuthorEmail() == null) versionDoc.setAuthorEmail(prev.getAuthorEmail());
                if (versionDoc.getDescription() == null) versionDoc.setDescription(prev.getDescription());
                if (versionDoc.getPackageURL() == null) versionDoc.setPackageURL(prev.getPackageURL());
                if (versionDoc.getDocumentationURL() == null) versionDoc.setDocumentationURL(prev.getDocumentationURL());
            }
        }

        // Save of the document
        packageVersionRepository.save(versionDoc);


        // --- Write on Neo4j ---
        String neo4jVersionId = packageName + " " + version;
        try {
            if (!packageGraphRepository.existsById(packageName)) {
                return new Result.Failure<>(new DomainError.NotFound("Package " + packageName + " not found in the system."));
            }

            PackageVersionNode versionNode = new PackageVersionNode();
            versionNode.setId(neo4jVersionId);
            versionNode.setVersion(version);
            versionNode.setIsStub(false);
            
            // Parsing the version
            try {
                String[] parts = version.split("\\.");
                if (parts.length > 0) versionNode.setMajor(Integer.parseInt(parts[0]));
                if (parts.length > 1) versionNode.setMinor(Integer.parseInt(parts[1]));
                if (parts.length > 2) versionNode.setPatch(Integer.parseInt(parts[2]));
            } catch (Exception e) { 
                log.warn("Failed to parse version {} into major.minor.patch", version);
                return new Result.Failure<>(new DomainError.InvalidOperation("Failed to parse version " + version));
            }

            packageVersionGraphRepository.save(versionNode);
            packageGraphRepository.addVersionToPackage(packageName, neo4jVersionId);

            if (newVersionDTO.getVulnerabilities() != null && !newVersionDTO.getVulnerabilities().isEmpty()) {
                List<String> cveIds = newVersionDTO.getVulnerabilities().stream()
                    .map(EmbeddedVulnerability::getCveId)
                    .toList();

                packageVersionGraphRepository.attachVulnerabilities(neo4jVersionId, cveIds);
            }

            if (newVersionDTO.getDependencies() != null && !newVersionDTO.getDependencies().isEmpty()) {
                List<Map<String, String>> parsedDeps = newVersionDTO.getDependencies().stream()
                    .map(this::parseDependencyForGraph) 
                    .toList();

                log.info("Linking {} dependencies with stubs logic for version {}", parsedDeps.size(), neo4jVersionId);
                packageVersionGraphRepository.attachDependenciesWithStubs(neo4jVersionId, parsedDeps);
            }

            log.info("Successfully published version {} with {} vulnerabilities", neo4jVersionId,
                    (newVersionDTO.getVulnerabilities() != null ? newVersionDTO.getVulnerabilities().size() : 0));

        } catch (Exception e) { 
            // Rollback MongoDB
            try {
                if (versionDoc.getId() != null) {
                    packageVersionRepository.deleteById(versionDoc.getId());
                    log.info("Rollback: Deleted version document from MongoDB");
                }
            } catch (Exception rollbackEx) {
                log.error("Failed to rollback MongoDB insertion", rollbackEx);
            }

            // If Neo4j write fails after creating the node, we have to delete the orphaned node
            try {
                if (packageVersionGraphRepository.existsById(neo4jVersionId)) {
                    packageVersionGraphRepository.deleteById(neo4jVersionId);
                    log.warn("Cleaned up zombie node {} from Neo4j", neo4jVersionId);
                }
            } catch (Exception neoEx) {
                log.debug("Neo4j cleanup skipped or failed (node might not exist).");
            }

            return new Result.Failure<>(new DomainError.SystemError("Failed to added new version.", e));
        }

        return new Result.Success<>(null);
    }

    // Update package's metadata (propagates to all versions)
    public Result<GeneralPackageDTO> updatePackageMetadata(String packageName, GeneralPackageDTO updateData) {
        log.info("Updating metadata for all the version of package: {}", packageName);

        // Find all the versions
        List<PackageVersion> allVersions = packageVersionRepository.findByPackageName(packageName);
        
        if (allVersions.isEmpty()) {
            return new Result.Failure<>(new DomainError.NotFound("Package " + packageName + " not found or has no versions."));
        }

        // Updating all the versions
        for (PackageVersion v : allVersions) {
            v.setAuthor(updateData.getAuthor());
            v.setAuthorEmail(updateData.getAuthorEmail());
            v.setDescription(updateData.getDescription());
            v.setPackageURL(updateData.getPackageURL());
            v.setDocumentationURL(updateData.getDocumentationURL());
        }
        packageVersionRepository.saveAll(allVersions);

        // Return updated DTO of a random version (they are all the same)
        return new Result.Success<>(new GeneralPackageDTO(allVersions.get(0)));
    }

    // Delete a specific version of a package
    public Result<Void> deletePackageVersion(String packageName, String version) {
        String neo4jVersionId = packageName + " " + version;
        log.info("Deleting version {} of package {}", version, packageName);

        // Find on Mongo
        var versionDocOpt = packageVersionRepository.findByPackageNameAndVersion(packageName, version);
        if (versionDocOpt.isEmpty()) {
            return new Result.Failure<>(new DomainError.NotFound("Version " + version + " not found."));
        }
        PackageVersion versionDoc = versionDocOpt.get();

        // Delete on MongoDB
        packageVersionRepository.delete(versionDoc);

        // Delete from Neo4j
        try {
            if (packageVersionGraphRepository.existsById(neo4jVersionId)) {
                packageVersionGraphRepository.deleteById(neo4jVersionId);
            }
            log.info("Successfully deleted version {}", neo4jVersionId);

        } catch (Exception e) {
            // Rollback: Restore on Mongo
            log.error("Neo4j delete failed! Restoring data on Mongo...", e);
            try {
                // versionDoc.setId(null);   // In this way we force mongo to create a new document with different id ==> fewer conflicts about id already exists
                packageVersionRepository.save(versionDoc);
                log.info("Rollback successful. Data restored on Mongo.");
            } catch (Exception rollbackEx) {
                log.error("CRITICAL: Failed to rollback DELETE", rollbackEx);
                return new Result.Failure<>(new DomainError.SystemError("Critical inconsistency during deletion.", e));
            }
            return new Result.Failure<>(new DomainError.SystemError("Failed to delete from Graph DB.", e));
        }

        return new Result.Success<>(null);
    }

    private Map<String, String> parseDependencyForGraph(String rawDep) {
        Matcher matcher = DEP_PATTERN.matcher(rawDep.trim());
        Map<String, String> result = new HashMap<>();

        if (matcher.find()) {
            String pkgName = matcher.group(1);
            String operator = matcher.group(2); 
            String version = matcher.group(3); 

            if (version == null || version.isEmpty()) {
                version = "latest"; 
                operator = "ANY"; 
            } else if (operator == null) {
                operator = "=="; 
            }

            result.put("pkgName", pkgName);
            result.put("version", version);
            result.put("operator", operator);
            
            result.put("targetId", pkgName + " " + version);
        } else {
            result.put("pkgName", rawDep);
            result.put("version", "unknown");
            result.put("operator", "unknown");
            result.put("targetId", rawDep + " unknown");
        }
        return result;
    }

}

