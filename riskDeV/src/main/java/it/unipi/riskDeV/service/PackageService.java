package it.unipi.riskDeV.service;

import it.unipi.riskDeV.DTO.packageVersion.AddPackageVersionDTO;
import it.unipi.riskDeV.DTO.packageVersion.ConstraintsDTO;
import it.unipi.riskDeV.DTO.packageVersion.EmbeddedVulnerabilityDTO;
import it.unipi.riskDeV.DTO.packageVersion.PackageVersionDTO;
import it.unipi.riskDeV.DTO.packageVersion.PublishedVersionDTO;
import it.unipi.riskDeV.DTO.packageVersion.ReverseDependencyDTO;
import it.unipi.riskDeV.DTO.packageVersion.UpdateGeneralPackageDTO;
import it.unipi.riskDeV.DTO.packageVersion.UpdatePackageVersionDTO;
import it.unipi.riskDeV.async.events.PackageEvents;
import it.unipi.riskDeV.model.documentDB.Constraints;
import it.unipi.riskDeV.model.documentDB.EmbeddedVulnerability;
import it.unipi.riskDeV.model.documentDB.PackageVersion;
import it.unipi.riskDeV.model.graphDB.PackageVersionNode;
import it.unipi.riskDeV.repository.documentDB.PackageVersionRepository;
import it.unipi.riskDeV.repository.graphDB.PackageVersionGraphRepository;
import it.unipi.riskDeV.results.DomainError;
import it.unipi.riskDeV.results.Result;
import it.unipi.riskDeV.util.Helper;
import it.unipi.riskDeV.util.VersionParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PackageService {

    private final PackageVersionRepository packageVersionRepository;
    private final PackageVersionGraphRepository packageVersionGraphRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final VersionParser versionParser;
    private final Helper helper;

    // Returns information about a package using the latest available version.
    public Result<PackageVersionDTO> getPackageByName(String packageName) {
        var optionalPackage = packageVersionRepository.findTopByPackageNameOrderByVersionArrayDesc(packageName);

        if (optionalPackage.isPresent()) {
            return new Result.Success<>(new PackageVersionDTO(optionalPackage.get()));
        }

        // TODO: If the specified package does not exist, perform an API request to PyPI
        // to check whether the package is missing from the system or does not exist at all.

        return new Result.Failure<>(new DomainError.NotFound("Package " + packageName + " not found."));
    }

    // Returns information about a specific version of a package
    public Result<PackageVersionDTO> getPackageByNameVersion(String packageName, String packageVersion) {
        var optionalPackage = packageVersionRepository.findByPackageNameAndVersion(packageName, packageVersion);
            
        if (optionalPackage.isPresent()) {
            return new Result.Success<>(new PackageVersionDTO(optionalPackage.get()));
        }

        // TODO: If the specified package version does not exist, perform an API request to PyPI
        // to check whether the package is missing from the system or does not exist at all.

        return new Result.Failure<>(new DomainError.NotFound("Version " + packageVersion + " of package " + packageName + " not found."));
    }

    // Returns the direct dependencies of a specific package version.
    public Result<List<String>> getDirectDependencies(String packageName, String version) {
        var versionDocOpt = packageVersionRepository.findByPackageNameAndVersion(packageName, version);

        if (versionDocOpt.isEmpty()) {
            return new Result.Failure<>(new DomainError.NotFound("Package " + packageName + " version " + version + " not found in database."));
        }

        List<Constraints> rawDependencies = versionDocOpt.get().getDependencies();
        if (rawDependencies == null) {
            rawDependencies = new ArrayList<>();
        }

        List<String> result = new ArrayList<>();
        for(Constraints dependency: rawDependencies) {
            result.add(dependency.getFull());
        }

        return new Result.Success<>(result);
    }

    // Returns a list of package versions that depend on the given package version.
    public Result<List<ReverseDependencyDTO>> getPackagesDependingOn(String packageName, String version) {
        if (!packageVersionGraphRepository.existsByPackageNameAndVersion(packageName, version)) {
            return new Result.Failure<>(new DomainError.NotFound("Package " + packageName + " not found in the system."));
        }

        List<PackageVersionNode> dependents = packageVersionGraphRepository.findReverseDependencies(packageName, version);

        Map<String, List<String>> grouped = new HashMap<>();

        for (PackageVersionNode node : dependents) {
            String name = node.getPackageName();
            String ver = node.getVersion();
            grouped.computeIfAbsent(name, k -> new ArrayList<>()); // Initialize a new list if the package is not yet in the map
            grouped.get(name).add(ver);
        }

        List<ReverseDependencyDTO> result = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : grouped.entrySet()) {
            result.add(new ReverseDependencyDTO(entry.getKey(), entry.getValue()));
        }

        return new Result.Success<>(result);
    }

    // Returns the last version of the specified package that have no known vulnerabilities.
    public Result<PackageVersionDTO> getSafeVersions(String packageName) {
        if (!packageVersionRepository.existsByPackageName(packageName)) {
            return new Result.Failure<>(new DomainError.NotFound("Package " + packageName + " not found."));
        }

        var optionalSafeVersion = packageVersionRepository.findTopByPackageNameAndRiskScoreOrderByVersionArrayDesc(packageName, 0);
        
        PackageVersionDTO safeVersion = new PackageVersionDTO(optionalSafeVersion.get());
        
        return new Result.Success<>(safeVersion);
    }

    // Add a new version of a package.
    public Result<String> addNewVersion(String packageName, AddPackageVersionDTO newVersionDTO) {
        if (!packageName.equals(newVersionDTO.getPackageName())) {
            return new Result.Failure<>(new DomainError.InvalidOperation("The package name in the URL does not match the package name in the request body"));
        }
        
        String version = newVersionDTO.getVersion();
        log.info("Publishing version {} for package {}", version, packageName);

        if (packageVersionRepository.existsByPackageNameAndVersion(packageName, version)) {
            return new Result.Failure<>(new DomainError.AlreadyExists("Version " + version + " already exists."));
        }

        // TODO: Remove Helper and move the computation of risk_score to an asynchronous event        
        PackageVersion versionDoc = new PackageVersion(newVersionDTO, helper, versionParser);

        try {
            packageVersionRepository.save(versionDoc);
            log.info("Version document saved in MongoDB: {}", versionDoc.getId());

            PublishedVersionDTO publishedVersionDTO = new PublishedVersionDTO(versionDoc);
            eventPublisher.publishEvent(new PackageEvents.VersionReleaseEvent(publishedVersionDTO));
        } catch (Exception e) {
            log.error("Failed to save version in MongoDB.", e);
            return new Result.Failure<>(new DomainError.SystemError("Error while saving version. Please try again.", e));
        }

        return new Result.Success<>("Package created successfully");    
    }

    // Updates the general metadata of a package for all its versions.
    public Result<String> updatePackageMetadata(String packageName, UpdateGeneralPackageDTO updateData) {
        if (!packageVersionRepository.existsByPackageName(packageName)) {
            return new Result.Failure<>(new DomainError.NotFound("Package " + packageName + " not found."));
        }

        helper.updatePackageGeneralMetadata(packageName, updateData);

        if(updateData.getDocumentationURL().isPresent()) {
            eventPublisher.publishEvent(new PackageEvents.UpdateDocumentationEvent(packageName, updateData.getDocumentationURL().get()));
        }

        log.info("Metadata updated for {}", packageName);
        
        return new Result.Success<>("Update executed successfully");
    }

    // Updates the data of a specific package version.
    public Result<String> updatePackageVersion(String packageName, String version, UpdatePackageVersionDTO updateVersionDTO) {
        var existingOpt = packageVersionRepository.findByPackageNameAndVersion(packageName, version);
        
        if (existingOpt.isEmpty()) {
            return new Result.Failure<>(new DomainError.NotFound("Package " + packageName + " " + version + " not found."));
        }
        
        PackageVersion updateVersion = existingOpt.get();

        if (updateVersionDTO.getRequiresPython().isPresent()) {
            updateVersion.setRequiresPython(updateVersionDTO.getRequiresPython().get());
        }

        if (updateVersionDTO.getUploadTime().isPresent()) {
            updateVersion.setUploadTime(updateVersionDTO.getUploadTime().get());
        }

        if (updateVersionDTO.getDependencies() != null) {
            List<Constraints> dependencyList = new ArrayList<>();
            for(ConstraintsDTO tmp: updateVersionDTO.getDependencies()) {
                dependencyList.add(new Constraints(tmp));
            }
            updateVersion.setDependencies(dependencyList);
        }

        if (updateVersionDTO.getVulnerabilities() != null) {
            List<EmbeddedVulnerability> vulnerabiltyList = new ArrayList<>();
            for(EmbeddedVulnerabilityDTO tmp: updateVersionDTO.getVulnerabilities()) {
                vulnerabiltyList.add(new EmbeddedVulnerability(tmp));
            }
            updateVersion.setVulnerabilities(vulnerabiltyList);
        }

        try {
            packageVersionRepository.save(updateVersion);
            log.info("Update done on MongoDB");

            eventPublisher.publishEvent(new PackageEvents.UpdatePackageVersionEvent(packageName, version, updateVersionDTO.getDependencies(), updateVersionDTO.getVulnerabilities()));
        } catch (Exception e) {
            return new Result.Failure<>(new DomainError.SystemError("Failed to update the package on MongoDB", e));
        }

        return new Result.Success<>("Package updated successfully");
    }

    // Deletes a specific version of a package.
    public Result<String> deletePackageVersion(String packageName, String version) {
        log.info("Deleting version {} of package {}", version, packageName);

        packageVersionRepository.deleteByPackageNameAndVersion(packageName, version);

        log.info("Successfully deleted package from MongoDB");

        eventPublisher.publishEvent(
            new PackageEvents.DeletePackageVersionEvent(packageName, version)
        );

        return new Result.Success<>("Package deleted successfully");
    }    
}