package it.unipi.riskDeV.service;

import it.unipi.riskDeV.DTO.DependencyDTO;
import it.unipi.riskDeV.DTO.GeneralPackageDTO;
import it.unipi.riskDeV.DTO.PackageVersionDTO;
import it.unipi.riskDeV.common.DomainError;
import it.unipi.riskDeV.common.Result;
import it.unipi.riskDeV.model.PackageVersion;
import it.unipi.riskDeV.model.PackageVersion.EmbeddedVulnerability;
import it.unipi.riskDeV.model.neo4j.PackageNode;
import it.unipi.riskDeV.model.neo4j.PackageVersionNode;
import it.unipi.riskDeV.repository.PackageGraphRepository;
import it.unipi.riskDeV.repository.PackageVersionGraphRepository;
import it.unipi.riskDeV.repository.PackageVersionRepository;
import it.unipi.riskDeV.APIClient.PyPiApiClient;
import it.unipi.riskDeV.APIClient.OsvApiClient;
import it.unipi.riskDeV.APIClient.PyPiMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class PackageService {

    private final PackageVersionRepository packageVersionRepository;
    private final PackageGraphRepository packageGraphRepository;
    private final PackageVersionGraphRepository packageVersionGraphRepository;
    private final MongoTemplate mongoTemplate;
    private final PyPiApiClient pyPiApiClient;
    private final OsvApiClient osvApiClient;
    private static final Pattern DEP_PATTERN = Pattern.compile("^([a-zA-Z0-9_\\-.]+)\\s*([<>=!~]+)?\\s*(.*)$");
    @Lazy
    private final PackageIngestionService packageIngestionService;

    // Get a package by its name (returns metadata from the latest version)
    public Result<PackageVersionDTO> getPackageByName(String packageName) {
        try {
            var pkgOpt = packageVersionRepository.findTopByPackageNameOrderByVersionArrayDesc(packageName);

            if (pkgOpt.isPresent()) {
                return new Result.Success<>(new PackageVersionDTO(pkgOpt.get()));
            }

            // Fetching on PyPi
            log.info("Package {} not found locally. Searching on PyPI...", packageName);
            var pypiResponse = pyPiApiClient.getPackageMetadata(packageName);

            if (pypiResponse.isPresent()) {
                // Map the response
                PackageVersionDTO newVersionDTO = PyPiMapper.toPackageVersionDTO(pypiResponse.get());

                // Get vulnerability list with osv (empty list if osv doesn't work)
                List<EmbeddedVulnerability> vulns = osvApiClient.getVulnerabilities(
                    newVersionDTO.getPackageName(), 
                    newVersionDTO.getVersion()
                );
                newVersionDTO.setVulnerabilities(vulns);
                
                // Saving on Mongo and Neo4j
                Result<Void> saveResult = addNewVersion(packageName, newVersionDTO);

                if (saveResult instanceof Result.Failure) {
                    return new Result.Failure<>(((Result.Failure<Void>) saveResult).error());
                }
                // Package name added to the queue of missing package
                packageIngestionService.enqueuePackage(packageName);
                return new Result.Success<>(newVersionDTO);
            }

            return new Result.Failure<>(new DomainError.NotFound("Package " + packageName + " not found locally or on PyPI."));

        } catch (Exception e) {
            log.error("Error fetching package {} from MongoDB/PyPI", packageName, e);
            return new Result.Failure<>(new DomainError.SystemError("Database/API error while fetching package", e));
        }
    }

    // Get information about a specific version of a package
    public Result<PackageVersionDTO> getPackageByNameVersion(String packageName, String packageVersion) {
        try {
            var pkgVerOpt = packageVersionRepository.findByPackageNameAndVersion(packageName, packageVersion);
            
            if (pkgVerOpt.isPresent()) {
                return new Result.Success<>(new PackageVersionDTO(pkgVerOpt.get()));
            }

            // Fetch on PyPi
            log.info("Version {} of {} not found locally. Searching on PyPI...", packageVersion, packageName);
            var pypiResponse = pyPiApiClient.getPackageVersionMetadata(packageName, packageVersion);

            if (pypiResponse.isPresent()) {
                // Map the response
                PackageVersionDTO newVersionDTO = PyPiMapper.toPackageVersionDTO(pypiResponse.get());

                // Vulns request to osv (empty list if osv doens't work)
                List<EmbeddedVulnerability> vulns = osvApiClient.getVulnerabilities(packageName, packageVersion);
                newVersionDTO.setVulnerabilities(vulns);
                
                // Save on MongoDb and Neo4j
                Result<Void> saveResult = addNewVersion(packageName, newVersionDTO);
                
                if (saveResult instanceof Result.Failure) {
                    return new Result.Failure<>(((Result.Failure<Void>) saveResult).error());
                }

                // Package name added to the queue of missing package
                packageIngestionService.enqueuePackage(packageName);

                return new Result.Success<>(newVersionDTO);
            }

            return new Result.Failure<>(new DomainError.NotFound("Version " + packageVersion + " of package " + packageName + " not found locally or on PyPI."));

        } catch (Exception e) {
            log.error("Error fetching version {} of package {}", packageVersion, packageName, e);
            return new Result.Failure<>(new DomainError.SystemError("Database/API error while fetching version", e));
        }
    }

    // Get all the dependencies required by a package
    public Result<List<String>> getPackagesDependingOn(String packageName) {
        log.info("Searching for packages depending on: {}", packageName);

        try {
            // Checking if the package exists on neo4j
            if (!packageGraphRepository.existsById(packageName)) {
                return new Result.Failure<>(new DomainError.NotFound("Package " + packageName + " not found in the system."));
            }

            var dependents = packageVersionGraphRepository.findReverseDependencies(packageName);
            var ids = dependents.stream().map(PackageVersionNode::getId).toList();
            return new Result.Success<>(ids);

        } catch (Exception e) {
            log.error("Neo4j error fetching dependents for {}", packageName, e);
            return new Result.Failure<>(new DomainError.SystemError("Neo4j error fetching dependents", e));
        }
    }

    // Get all the dependencies of a specific package
    public Result<List<DependencyDTO>> getDirectDependencies(String packageName, String version) {
        String versionId = packageName + " " + version;
        log.info("Searching for direct dependencies of: {}", versionId);

        try {
            // Search in the graph
            if (packageVersionGraphRepository.existsById(versionId)) {
                List<DependencyDTO> dependencies = packageVersionGraphRepository.findDirectDependencies(versionId);
                return new Result.Success<>(dependencies);
            }

            // Calling Pypi api
            log.info("Dependencies for {} not found locally. Searching on PyPI...", versionId);
            var pypiResponse = pyPiApiClient.getPackageVersionMetadata(packageName, version);

            if (pypiResponse.isPresent()) {
                // Save in MongoDB and Neo4j
                PackageVersionDTO newVersionDTO = PyPiMapper.toPackageVersionDTO(pypiResponse.get());
                List<EmbeddedVulnerability> vulns = osvApiClient.getVulnerabilities(packageName, version);
                newVersionDTO.setVulnerabilities(vulns);
                
                Result<Void> saveResult = addNewVersion(packageName, newVersionDTO);

                if (saveResult instanceof Result.Failure) {
                    return new Result.Failure<>(((Result.Failure<Void>) saveResult).error());
                }
                // Package name added to the queue of missing package
                packageIngestionService.enqueuePackage(packageName);

                // Calling the query
                List<DependencyDTO> dependencies = packageVersionGraphRepository.findDirectDependencies(versionId);
                return new Result.Success<>(dependencies);
            }

            return new Result.Failure<>(new DomainError.NotFound("Package version " + versionId + " not found locally or on PyPI."));

        } catch (Exception e) {
            log.error("Error fetching direct dependencies for {}", versionId, e);
            return new Result.Failure<>(new DomainError.SystemError("Error fetching dependencies", e));
        }
    }

    // Get versions without CVEs of a specific package
    public Result<List<PackageVersionDTO>> getSafeVersions(String packageName) {
        log.info("Searching for safe versions of package: {}", packageName);

        try {
            // Usa exists di Mongo o Neo4j? Meglio Mongo qui che è la source of truth dei metadati
            if (!packageVersionRepository.existsByPackageName(packageName)) {
                return new Result.Failure<>(new DomainError.NotFound("Package " + packageName + " not found."));
            }

            List<PackageVersion> safeVersions = packageVersionRepository.findSafeVersions(packageName);
            var versions = safeVersions.stream().map(PackageVersionDTO::new).toList();
            return new Result.Success<>(versions);

        } catch (Exception e) {
            log.error("Error fetching safe versions for {}", packageName, e);
            return new Result.Failure<>(new DomainError.SystemError("Database error fetching safe versions", e));
        }
    }

    // Add a new version of a package
    public Result<Void> addNewVersion(String packageName, PackageVersionDTO newVersionDTO) {
        String version = newVersionDTO.getVersion();
        log.info("Publishing version {} for package {}", version, packageName);

        if (packageVersionRepository.existsByPackageNameAndVersion(packageName, version)) {
            return new Result.Failure<>(new DomainError.AlreadyExists("Version " + version + " already exists."));
        }

        // Document preparation
        PackageVersion versionDoc = new PackageVersion();
        versionDoc.setPackageName(packageName);
        versionDoc.setVersion(version);
        versionDoc.setVersionArray(generateVersionArray(version)); 
        versionDoc.setUploadTime(Instant.now().toString());
        versionDoc.setRequiresPython(newVersionDTO.getRequiresPython());
        versionDoc.setDependencies(newVersionDTO.getDependencies() != null ? newVersionDTO.getDependencies() : new ArrayList<>());
        versionDoc.setVulnerabilities(newVersionDTO.getVulnerabilities() != null ? newVersionDTO.getVulnerabilities() : new ArrayList<>());
        
        // Metadata
        versionDoc.setAuthor(newVersionDTO.getAuthor());
        versionDoc.setAuthorEmail(newVersionDTO.getAuthorEmail());
        versionDoc.setDescription(newVersionDTO.getDescription());
        versionDoc.setPackageURL(newVersionDTO.getPackageURL());
        versionDoc.setDocumentationURL(newVersionDTO.getDocumentationURL());

        // Fallback Metadata
        try {
            if (versionDoc.getAuthor() == null || versionDoc.getDescription() == null) {
                var prevOpt = packageVersionRepository.findTopByPackageNameOrderByVersionArrayDesc(packageName);
                if (prevOpt.isPresent()) {
                    PackageVersion prev = prevOpt.get();
                    if (versionDoc.getAuthor() == null) versionDoc.setAuthor(prev.getAuthor());
                    if (versionDoc.getAuthorEmail() == null) versionDoc.setAuthorEmail(prev.getAuthorEmail());
                    if (versionDoc.getDescription() == null) versionDoc.setDescription(prev.getDescription());
                    if (versionDoc.getPackageURL() == null) versionDoc.setPackageURL(prev.getPackageURL());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch previous version metadata, proceeding without fallback", e);
        }

        // Save on MongoDB
        try {
            packageVersionRepository.save(versionDoc);
            log.info("Version document saved in MongoDB: {}", versionDoc.getId());
        } catch (Exception e) {
            log.error("Failed to save version in MongoDB.", e);
            return new Result.Failure<>(new DomainError.SystemError("Error while saving version. Please try again.", e));
        }

        // Write on Neo4j
        String neo4jVersionId = packageName + " " + version;
        try {
            if (!packageGraphRepository.existsById(packageName)) {
                PackageNode newPkg = new PackageNode();
                newPkg.setId(packageName);
                packageGraphRepository.save(newPkg);
                log.info("Auto-created missing parent package node: {}", packageName);
            }

            PackageVersionNode versionNode = new PackageVersionNode();
            versionNode.setId(neo4jVersionId);
            versionNode.setVersion(version);
            versionNode.setIsStub(false);

            try {
                List<Integer> vArray = versionDoc.getVersionArray();
                if (vArray.size() >= 1) versionNode.setMajor(vArray.get(0));
                if (vArray.size() >= 2) versionNode.setMinor(vArray.get(1));
                if (vArray.size() >= 3) versionNode.setPatch(vArray.get(2));
            } catch (Exception e) {
                log.warn("Complex version number, skipping major/minor mapping for Neo4j: {}", version);
            }

            packageVersionGraphRepository.save(versionNode);
            packageGraphRepository.addVersionToPackage(packageName, neo4jVersionId);

            if (!versionDoc.getVulnerabilities().isEmpty()) {
                List<String> cveIds = versionDoc.getVulnerabilities().stream()
                    .map(EmbeddedVulnerability::getCveId)
                    .toList();
                packageVersionGraphRepository.attachVulnerabilities(neo4jVersionId, cveIds);
            }

            if (!versionDoc.getDependencies().isEmpty()) {
                List<Map<String, String>> parsedDeps = versionDoc.getDependencies().stream()
                    .map(this::parseDependencyForGraph) 
                    .toList();
                packageVersionGraphRepository.attachDependenciesWithStubs(neo4jVersionId, parsedDeps);
            }

        } catch (Exception e) {
            log.error("Neo4j write failed. Rolling back MongoDB...", e);
            try {
                packageVersionRepository.deleteById(versionDoc.getId());
                log.info("Rolled back MongoDB version: {}", versionDoc.getId());
            } catch (Exception ex) {
                log.error("Failed to rollback MongoDB after Neo4j failure.", ex);
            }
            return new Result.Failure<>(new DomainError.SystemError("Failed to publish version on Graph DB", e));
        }

        return new Result.Success<>(null);
    }

    // Update package's metadata (propagates to all versions)
    public Result<GeneralPackageDTO> updatePackageMetadata(String packageName, GeneralPackageDTO updateData) {
        log.info("Updating metadata for ALL versions of package: {}", packageName);

        try {
            if (!packageVersionRepository.existsByPackageName(packageName)) {
                return new Result.Failure<>(new DomainError.NotFound("Package " + packageName + " not found."));
            }

            Query query = new Query(Criteria.where("package_name").is(packageName));
            Update update = new Update()
                    .set("author", updateData.getAuthor())
                    .set("author_email", updateData.getAuthorEmail())
                    .set("description", updateData.getDescription())
                    .set("package_url", updateData.getPackageURL())
                    .set("documentation", updateData.getDocumentationURL());

            var updateResult = mongoTemplate.updateMulti(query, update, PackageVersion.class);
            log.info("Metadata updated for {} versions of package {}", updateResult.getModifiedCount(), packageName);
            
            return new Result.Success<>(updateData);

        } catch (Exception e) {
            log.error("Error updating metadata for {}", packageName, e);
            return new Result.Failure<>(new DomainError.SystemError("Database error during update", e));
        }
    }

    // Update a version of a package
    public Result<PackageVersionDTO> updatePackageVersion(String packageName, String version, PackageVersionDTO updateDTO) {
        log.info("Updating specific version details for: {} {}", packageName, version);

        try {
            var existingOpt = packageVersionRepository.findByPackageNameAndVersion(packageName, version);
            if (existingOpt.isEmpty()) {
                return new Result.Failure<>(new DomainError.NotFound("Version " + version + " not found."));
            }
            PackageVersion doc = existingOpt.get();

            doc.setRequiresPython(updateDTO.getRequiresPython());
            doc.setRiskScore(updateDTO.getRiskScore());
            doc.setDependencies(updateDTO.getDependencies() != null ? updateDTO.getDependencies() : new ArrayList<>());
            doc.setVulnerabilities(updateDTO.getVulnerabilities() != null ? updateDTO.getVulnerabilities() : new ArrayList<>());

            packageVersionRepository.save(doc); // Mongo Save

            // Neo4j Sync
            String neo4jId = packageName + " " + version;
            try {
                if (doc.getDependencies() != null) {
                    packageVersionGraphRepository.deleteDependencies(neo4jId);
                    List<Map<String, String>> parsedDeps = doc.getDependencies().stream()
                            .map(this::parseDependencyForGraph)
                            .toList();
                    if (!parsedDeps.isEmpty()) {
                        packageVersionGraphRepository.attachDependenciesWithStubs(neo4jId, parsedDeps);
                    }
                }

                if (doc.getVulnerabilities() != null) {
                    packageVersionGraphRepository.deleteVulnerabilities(neo4jId);
                    List<String> cveIds = doc.getVulnerabilities().stream()
                            .map(EmbeddedVulnerability::getCveId)
                            .toList();
                    if (!cveIds.isEmpty()) {
                        packageVersionGraphRepository.attachVulnerabilities(neo4jId, cveIds);
                    }
                }
            } catch (Exception e) {  
                log.error("Neo4j alignment failed for updated version {}", neo4jId, e);
                // Note: Partial rollback is hard here without old data backup.
                // Keeping consistent with team style: Log and Fail.
                return new Result.Failure<>(new DomainError.SystemError("Update saved on DB but Graph sync failed.", e));
            }

            return new Result.Success<>(new PackageVersionDTO(doc));

        } catch (Exception e) {
            log.error("Error updating version document for {} {}", packageName, version, e);
            return new Result.Failure<>(new DomainError.SystemError("Database error during version update", e));
        }
    }

    // Delete a specific version of a package
    public Result<Void> deletePackageVersion(String packageName, String version) {
        String neo4jVersionId = packageName + " " + version;
        log.info("Deleting version {} of package {}", version, packageName);

        try {
            var versionDocOpt = packageVersionRepository.findByPackageNameAndVersion(packageName, version);
            if (versionDocOpt.isEmpty()) {
                return new Result.Failure<>(new DomainError.NotFound("Version not found."));
            }
            PackageVersion versionDoc = versionDocOpt.get();

            packageVersionRepository.delete(versionDoc);

            try {
                if (packageVersionGraphRepository.existsById(neo4jVersionId)) {
                    packageVersionGraphRepository.deleteById(neo4jVersionId);
                }
            } catch (Exception e) {
                log.error("Neo4j delete failed! Restoring data on Mongo...", e);
                try {
                    packageVersionRepository.save(versionDoc); 
                } catch (Exception restoreEx) {
                    log.error("CRITICAL: Failed to restore MongoDB data after Neo4j delete failure", restoreEx);
                }
                return new Result.Failure<>(new DomainError.SystemError("Failed to delete from Graph DB.", e));
            }

            return new Result.Success<>(null);

        } catch (Exception e) {
            log.error("Error deleting version {} {}", packageName, version, e);
            return new Result.Failure<>(new DomainError.SystemError("Database error during deletion", e));
        }
    }

    // PRIVATE METHODS

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

    private static final Map<String, Integer> VERSION_WEIGHTS = Map.of(
        "dev", -4, "alpha", -3, "a", -3, "beta", -2, "b", -2, 
        "rc", -1, "c", -1, "pre", -1, "post", 1, "pl", 1
    );

    private List<Integer> generateVersionArray(String versionStr) {
        List<Integer> normalized = new ArrayList<>();
        if (versionStr == null) return List.of(0, 0, 0, 0, 0, 0);

        Matcher m = Pattern.compile("(\\d+|[a-z]+)").matcher(versionStr.toLowerCase());
        
        while (m.find()) {
            String part = m.group();
            if (part.matches("\\d+")) {
                normalized.add(Integer.parseInt(part));
            } else {
                normalized.add(VERSION_WEIGHTS.getOrDefault(part, -5));
            }
        }

        while (normalized.size() < 6) {
            normalized.add(0);
        }
        
        return normalized.subList(0, 6);
    }
}