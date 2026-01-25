package it.unipi.riskDeV.service;

import it.unipi.riskDeV.DTO.GeneralPackageDTO;
import it.unipi.riskDeV.DTO.PackageVersionDTO;
import it.unipi.riskDeV.DTO.PublishedVersionDTO;
import it.unipi.riskDeV.DTO.ReverseDependencyDTO;
import it.unipi.riskDeV.DTO.UpdatePackageVersionDTO;
import it.unipi.riskDeV.common.DomainError;
import it.unipi.riskDeV.common.Result;
import it.unipi.riskDeV.event.PackageEvent;
import it.unipi.riskDeV.model.PackageVersion;
import it.unipi.riskDeV.model.PackageVersion.Constraints;
import it.unipi.riskDeV.repository.PackageVersionGraphRepository;
import it.unipi.riskDeV.repository.PackageVersionRepository;
import it.unipi.riskDeV.util.Helper;
import it.unipi.riskDeV.util.Utility;
// import it.unipi.riskDeV.APIClient.PyPiApiClient;
// import it.unipi.riskDeV.APIClient.OsvApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.ApplicationEventPublisher;
// import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PackageService {

    private final PackageVersionRepository packageVersionRepository;
    private final PackageVersionGraphRepository packageVersionGraphRepository;
    // private final PyPiApiClient pyPiApiClient;
    // private final OsvApiClient osvApiClient;
    // @Lazy
    // private final PackageIngestionService packageIngestionService;
    private final ApplicationEventPublisher eventPublisher;
    private final Utility util;
    private final Helper helper;

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
    // TODO: NON FUNZIONA
    public Result<List<ReverseDependencyDTO>> getPackagesDependingOn(String packageName, String version) {
        log.info("Searching for packages depending on: {}", packageName);

        if (!packageVersionGraphRepository.existsByPackageNameAndVersion(packageName, version)) {
            return new Result.Failure<>(new DomainError.NotFound("Package " + packageName + " not found in the system."));
        }

        var optionalDependents = packageVersionGraphRepository.findReverseDependencies(packageName, version);

        //TODO: fare DTO
        List<ReverseDependencyDTO> result = optionalDependents.get();
    
        return new Result.Success<>(result);
    }

    // Get all the dependencies of a specific package
    // TODO: Sistemare output perchè ci sono troppi null
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
    // TODO: Sistemare output perchè ci sono troppi null
    public Result<List<PackageVersionDTO>> getSafeVersions(String packageName) {
        log.info("Searching for safe versions of package: {}", packageName);

        if (!packageVersionRepository.existsByPackageName(packageName)) {
            return new Result.Failure<>(new DomainError.NotFound("Package " + packageName + " not found."));
        }

        var optionalSafeVersions = packageVersionRepository.findTopByPackageNameAndRiskScoreOrderByVersionArrayDesc(packageName, 0);
        
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
        versionDoc.setVersionArray(util.generateVersionArray(version)); 

        // Save on MongoDB
        try {
            packageVersionRepository.save(versionDoc);
            log.info("Version document saved in MongoDB: {}", versionDoc.getId());
            PublishedVersionDTO publishedVersionDTO = new PublishedVersionDTO(versionDoc);
            eventPublisher.publishEvent(new PackageEvent.VersionReleaseEvent(publishedVersionDTO));
        } catch (Exception e) {
            log.error("Failed to save version in MongoDB.", e);
            return new Result.Failure<>(new DomainError.SystemError("Error while saving version. Please try again.", e));
        }

        return new Result.Success<>("Package created successfully");    
}

    // Update package's metadata (propagates to all versions)
    public Result<String> updatePackageMetadata(String packageName, GeneralPackageDTO updateData) {
        log.info("Updating metadata for ALL versions of package: {}", packageName);
        
        if (!packageVersionRepository.existsByPackageName(packageName)) {
            return new Result.Failure<>(new DomainError.NotFound("Package " + packageName + " not found."));
        }

        helper.updatePackageGeneralMetadata(packageName, updateData);

        eventPublisher.publishEvent(new PackageEvent.UpdateDocumentationEvent(packageName, updateData.getDocumentationURL()));

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
            log.info("Update done on MongoDB");
            eventPublisher.publishEvent(new PackageEvent.UpdatePackageVersionEvent(packageName, version, updateVersionDTO.getDependencies(), updateVersionDTO.getVulnerabilities()));
        } catch (Exception e) {
            return new Result.Failure<>(new DomainError.SystemError("Failed to update the package on MongoDB", e));
        }

        return new Result.Success<>("Package updated successfully");
    }

    // Delete a specific version of a package
    public Result<String> deletePackageVersion(String packageName, String version) {
        log.info("Deleting version {} of package {}", version, packageName);

        packageVersionRepository.deleteByPackageNameAndVersion(packageName, version);

        log.info("Successfully deleted package from MongoDB");

        eventPublisher.publishEvent(new PackageEvent.DeletePackageVersionEvent(packageName, version));

        return new Result.Success<>("Package deleted successfully");
    }    
}