package it.unipi.riskDeV.service;

import it.unipi.riskDeV.DTO.GeneralPackageDTO;
import it.unipi.riskDeV.DTO.PackageVersionDTO;
import it.unipi.riskDeV.DTO.PackageVersionGraphDTO;
import it.unipi.riskDeV.DTO.UpdatePackageVersionDTO;
import it.unipi.riskDeV.common.DomainError;
import it.unipi.riskDeV.common.Result;
import it.unipi.riskDeV.model.PackageVersion;
import it.unipi.riskDeV.model.PackageVersion.EmbeddedVulnerability;
import it.unipi.riskDeV.model.PackageVersion.Constraints;
import it.unipi.riskDeV.model.neo4j.PackageNode;
import it.unipi.riskDeV.model.neo4j.PackageVersionNode;
import it.unipi.riskDeV.repository.PackageGraphRepository;
import it.unipi.riskDeV.repository.PackageVersionGraphRepository;
import it.unipi.riskDeV.repository.PackageVersionRepository;
// import it.unipi.riskDeV.APIClient.PyPiApiClient;
// import it.unipi.riskDeV.APIClient.OsvApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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
    // private final PyPiApiClient pyPiApiClient;
    // private final OsvApiClient osvApiClient;
    @Lazy
    private final PackageIngestionService packageIngestionService;
    private final MongoTemplate mongoTemplate;

    private static final Map<String, Integer> VERSION_WEIGHTS = Map.of(
        "dev", -4, "alpha", -3, "a", -3, "beta", -2, "b", -2, 
        "rc", -1, "c", -1, "pre", -1, "post", 1, "pl", 1
    );

    // Get a package by its name (returns metadata from the latest version)
    public Result<PackageVersionDTO> getPackageByName(String packageName) {
        var pkgOpt = packageVersionRepository.findTopByPackageNameOrderByVersionArrayDesc(packageName);

        if (pkgOpt.isPresent()) {
            return new Result.Success<>(new PackageVersionDTO(pkgOpt.get()));
        }
        return new Result.Failure<>(new DomainError.NotFound("Package " + packageName + " not found."));
    }
    /*
    public Result<PackageVersionDTO> getPackageByName(String packageName) {
        try {
            var pkgOpt = packageVersionRepository.findTopByPackageNameOrderByVersionArrayDesc(packageName);

            if (pkgOpt.isPresent()) {
                return new Result.Success<>(new PackageVersionDTO(pkgOpt.get()));
            }

            // TODO: API CALLS ACTUALLY DISABLED
            /*
            log.info("Package {} not found locally. Searching on PyPI...", packageName);
            var pypiResponse = pyPiApiClient.getPackageMetadata(packageName);

            if (pypiResponse.isPresent()) {
                PackageVersionDTO newVersionDTO = PyPiMapper.toPackageVersionDTO(pypiResponse.get());

                List<EmbeddedVulnerability> vulns = osvApiClient.getVulnerabilities(newVersionDTO.getPackageName(),newVersionDTO.getVersion());
                newVersionDTO.setVulnerabilities(vulns);

                Result<Void> saveResult = addNewVersion(packageName, newVersionDTO);
                if (saveResult instanceof Result.Failure) {
                    return new Result.Failure<>(((Result.Failure<Void>) saveResult).error());
                }

                packageIngestionService.enqueuePackage(packageName);
                return new Result.Success<>(newVersionDTO);
            }
            
            return new Result.Failure<>(new DomainError.NotFound("Package " + packageName + " not found."));

        } catch (Exception e) {
            log.error("Error fetching package {}", packageName, e);
            return new Result.Failure<>(new DomainError.SystemError("Database error while fetching package", e));
        }
    }*/

    // Get information about a specific version of a package
    // Get information about a specific version of a package
    public Result<PackageVersionDTO> getPackageByNameVersion(String packageName, String packageVersion) {
        var pkgVerOpt = packageVersionRepository.findByPackageNameAndVersion(packageName, packageVersion);
            
        if (pkgVerOpt.isPresent()) {
            return new Result.Success<>(new PackageVersionDTO(pkgVerOpt.get()));
        }

        return new Result.Failure<>(new DomainError.NotFound("Version " + packageVersion + " of package " + packageName + " not found."));
    }

    /*
    public Result<PackageVersionDTO> getPackageByNameVersion(String packageName, String packageVersion) {
        try {
            var pkgVerOpt = packageVersionRepository.findByPackageNameAndVersion(packageName, packageVersion);
            
            if (pkgVerOpt.isPresent()) {
                return new Result.Success<>(new PackageVersionDTO(pkgVerOpt.get()));
            }

            // TODO: API CALLS ACTUALLY DISABLED
            /*
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
            
            return new Result.Failure<>(new DomainError.NotFound("Version " + packageVersion + " of package " + packageName + " not found."));

        } catch (Exception e) {
            log.error("Error fetching version {} of package {}", packageVersion, packageName, e);
            return new Result.Failure<>(new DomainError.SystemError("Error while fetching version", e));
        }
    }*/

    // Retrieves all packages that directly depend on the specified package version.
    public Result<List<PackageVersionGraphDTO>> getPackagesDependingOn(String packageName, String version) {
        log.info("Searching for packages depending on: {}", packageName);

        if (!packageVersionGraphRepository.existsByPackageNameAndVersion(packageName, version)) {
            return new Result.Failure<>(new DomainError.NotFound("Package " + packageName + " not found in the system."));
        }

        var optionalDependents = packageVersionGraphRepository.findReverseDependencies(packageName, version);
        
        List<PackageVersionGraphDTO> ListDTO = optionalDependents.orElse(List.of()).stream().map(PackageVersionGraphDTO::new).toList();

        return new Result.Success<>(ListDTO);
    }

    // Get all the dependencies of a specific package
    public Result<List<Constraints>> getDirectDependencies(String packageName, String version) {
        
        var versionDocOpt = packageVersionRepository.findByPackageNameAndVersion(packageName, version);

        if (versionDocOpt.isEmpty()) {
            return new Result.Failure<>(new DomainError.NotFound("Package " + packageName + " version " + version + " not found in database."));
        }

        List<Constraints> rawDependencies = versionDocOpt.get().getDependencies();

        if (rawDependencies == null) {
            rawDependencies = new ArrayList<>();
        }

        return new Result.Success<>(rawDependencies);
    }

    // Get versions without CVEs of a specific package
    public Result<List<PackageVersionDTO>> getSafeVersions(String packageName) {
        log.info("Searching for safe versions of package: {}", packageName);

        if (!packageVersionRepository.existsByPackageName(packageName)) {
            return new Result.Failure<>(new DomainError.NotFound("Package " + packageName + " not found."));
        }

        var optionalSafeVersions = packageVersionRepository.findTop5ByPackageNameAndRiskScoreOrderByVersionArrayDesc(packageName, 0);
        
        var safeVersions = optionalSafeVersions.orElse(List.of()).stream().map(PackageVersionDTO::new).toList();
        
        return new Result.Success<>(safeVersions);
    }


    public Result<String> addNewVersion(String packageName, PackageVersionDTO newVersionDTO) {
        
        if(!packageName.equals(newVersionDTO.getPackageName())) {
            return new Result.Failure<>(new DomainError.InvalidOperation("The package name in the URL does not match the package name in the request body"));
        }
        
        String version = newVersionDTO.getVersion();
        log.info("Publishing version {} for package {}", version, packageName);

        if (packageVersionRepository.existsByPackageNameAndVersion(packageName, version)) {
            return new Result.Failure<>(new DomainError.AlreadyExists("Version " + version + " already exists."));
        }

        // Document preparation
        PackageVersion versionDoc = new PackageVersion(newVersionDTO);
        versionDoc.setVersionArray(generateVersionArray(version)); 

        // Save on MongoDB
        try {
            packageVersionRepository.save(versionDoc);
            log.info("Version document saved in MongoDB: {}", versionDoc.getId());
        } catch (Exception e) {
            log.error("Failed to save version in MongoDB.", e);
            return new Result.Failure<>(new DomainError.SystemError("Error while saving version. Please try again.", e));
        }

        try {
            if (!packageGraphRepository.existsByPackageName(packageName)) {
                PackageNode newPkg = new PackageNode();
                newPkg.setPackageName(packageName);
                packageGraphRepository.save(newPkg);
            }

            PackageVersionNode versionNode = new PackageVersionNode(versionDoc);

            packageVersionGraphRepository.save(versionNode);

            packageGraphRepository.addVersionToPackage(packageName, versionNode.getVersion());

            List<EmbeddedVulnerability> vulnerabilityList = versionDoc.getVulnerabilities();
            for (int i = 0; i < vulnerabilityList.size(); i++) {
                packageVersionGraphRepository.attachVulnerability(packageName, version, vulnerabilityList.get(i).getCveId());
            }

            List<Constraints> dependecesList = versionDoc.getDependencies();
            addDependeciesGraph(versionNode.getPackageName(), versionNode.getVersion(), dependecesList);

            Criteria dependencyCriteria = Criteria.where("requires_dist").elemMatch(
            Criteria.where("name").is(versionNode.getPackageName())
                    .orOperator(
                        Criteria.where("version_gte").lte(versionNode.getVersion()),
                        Criteria.where("version_gte").is(null)
                    )
                    .orOperator(
                        Criteria.where("version_lte").gte(versionNode.getVersion()),
                        Criteria.where("version_lte").is(null)
                    )
                    .orOperator(
                        Criteria.where("version_gt").lt(versionNode.getVersion()),
                        Criteria.where("version_gt").is(null)
                    )
                    .orOperator(
                        Criteria.where("version_lt").gt(versionNode.getVersion()),
                        Criteria.where("version_lt").is(null)
                    )
                    .orOperator(
                        Criteria.where("version_eq").is(versionNode.getVersion()),
                        Criteria.where("version_eq").is(null)
                    )
                    .orOperator(
                        Criteria.where("version_neq").ne(versionNode.getVersion()),
                        Criteria.where("version_neq").is(null)
                    )
            );

            Query query = new Query(dependencyCriteria);
            List<PackageVersion> dependents = mongoTemplate.find(query, PackageVersion.class);

            for(int i = 0; i < dependents.size(); i++) {
                packageVersionGraphRepository.attachDependency(dependents.get(i).getPackageName(), dependents.get(i).getVersion(), packageName, version);
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

    return new Result.Success<>("Package created successfully");    
}

    // Update package's metadata (propagates to all versions)
    public Result<String> updatePackageMetadata(String packageName, GeneralPackageDTO updateData) {
        log.info("Updating metadata for ALL versions of package: {}", packageName);
        
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

        mongoTemplate.updateMulti(query, update, PackageVersion.class);

        packageVersionGraphRepository.updateDocumentation(packageName, updateData.getDocumentationURL());

        log.info("Metadata updated for {}", packageName);
        
        return new Result.Success<>("Update executed successfully");
    }

    // Update a version of a package
    public Result<String> updatePackageVersion(String packageName, String version, UpdatePackageVersionDTO updateVersionDTO) {
        
        log.info("Updating specific version details for: {} {}", packageName, version);

        var existingOpt = packageVersionRepository.findByPackageNameAndVersion(packageName, version);
        if (existingOpt.isEmpty()) {
            return new Result.Failure<>(new DomainError.NotFound("Package " + packageName + " " + version + " not found."));
        }
        
        PackageVersion updateVersion  = existingOpt.get();

        if(updateVersionDTO.getRequiresPython() != null) {
            updateVersion.setRequiresPython(updateVersionDTO.getRequiresPython());
        }

        if(updateVersionDTO.getUploadTime() != null) {
            updateVersion.setUploadTime(updateVersionDTO.getUploadTime());
        }

        if(updateVersionDTO.getDependencies() != null) {
            updateVersion.setDependencies(updateVersionDTO.getDependencies());
        }

        if(updateVersionDTO.getVulnerabilities() != null) {
            updateVersion.setVulnerabilities(updateVersionDTO.getVulnerabilities());
        }

        try {
            packageVersionRepository.save(updateVersion);
        } catch (Exception e) {
            return new Result.Failure<>(new DomainError.SystemError("Failed to update the package on MongoDB", e));
        }
        
        if(updateVersionDTO.getDependencies() != null) {
            updateVersion.setDependencies(updateVersionDTO.getDependencies());
            packageVersionGraphRepository.deleteDependencies(packageName, version);
            addDependeciesGraph(packageName, version, updateVersionDTO.getDependencies());
        }

        if(updateVersionDTO.getVulnerabilities() != null) {
            updateVersion.setVulnerabilities(updateVersionDTO.getVulnerabilities());
            packageVersionGraphRepository.deleteVulnerabilities(packageName, version);

            List<EmbeddedVulnerability> vulnerabilityList = updateVersionDTO.getVulnerabilities();
            for (int i = 0; i < vulnerabilityList.size(); i++) {
                packageVersionGraphRepository.attachVulnerability(packageName, version, vulnerabilityList.get(i).getCveId());
            }
        }

        return new Result.Success<>("Package updated successfully");
    }

    // Delete a specific version of a package
    public Result<String> deletePackageVersion(String packageName, String version) {
        log.info("Deleting version {} of package {}", version, packageName);

        packageVersionRepository.deleteByPackageNameAndVersion(packageName, version);

        packageVersionGraphRepository.deleteByPackageNameAndVersion(packageName, version);

        return new Result.Success<>("Package deleted successfully");
    }

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

    private void addDependeciesGraph(String packageName, String version, List<Constraints> dependecesList) {
        for (int i = 0; i < dependecesList.size(); i++) {
            Constraints constraint = dependecesList.get(i);
            
            Criteria criteria = Criteria.where("package_name").is(constraint.getName());

            if (constraint.getVersion_gte() != null) {
                criteria = criteria.and("version_array").gte(generateVersionArray(constraint.getVersion_gte()));
            }
            if (constraint.getVersion_lte() != null) {
                criteria = criteria.and("version_array").lte(generateVersionArray(constraint.getVersion_lte()));
            }
            if (constraint.getVersion_gt() != null) {
                criteria = criteria.and("version_array").gt(generateVersionArray(constraint.getVersion_gt()));
            }
            if (constraint.getVersion_lt() != null) {
                criteria = criteria.and("version_array").lt(generateVersionArray(constraint.getVersion_lt()));
            }
            if (constraint.getVersion_eq() != null) {
                criteria = criteria.and("version_array").is(generateVersionArray(constraint.getVersion_eq()));
            }
            if (constraint.getVersion_neq() != null) {
                criteria = criteria.and("version_array").ne(generateVersionArray(constraint.getVersion_neq()));
            }

            Query query = new Query(criteria);

            List<PackageVersion> packageList = mongoTemplate.find(query, PackageVersion.class);

            for (int j = 0; j < packageList.size(); j++) {
                packageVersionGraphRepository.attachDependency(packageName, version, packageList.get(j).getPackageName(), packageList.get(j).getVersion());
            }

        }
    }
}