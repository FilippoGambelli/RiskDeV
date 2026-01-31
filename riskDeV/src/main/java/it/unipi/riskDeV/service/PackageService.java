package it.unipi.riskDeV.service;

import it.unipi.riskDeV.DTO.MessageResponseDTO;
import it.unipi.riskDeV.DTO.packageVersion.AddPackageVersionDTO;
import it.unipi.riskDeV.DTO.packageVersion.ConstraintsDTO;
import it.unipi.riskDeV.DTO.packageVersion.PackageVersionDTO;
import it.unipi.riskDeV.DTO.packageVersion.PublishedVersionDTO;
import it.unipi.riskDeV.DTO.packageVersion.ReverseDependencyDTO;
import it.unipi.riskDeV.DTO.packageVersion.UpdateGeneralPackageDTO;
import it.unipi.riskDeV.DTO.packageVersion.UpdatePackageVersionDTO;
import it.unipi.riskDeV.DTO.vulnerability.VulnerabilityReportDTO;
import it.unipi.riskDeV.async.events.PackageEvent;
import it.unipi.riskDeV.model.documentDB.Constraints;
import it.unipi.riskDeV.model.documentDB.EmbeddedVulnerability;
import it.unipi.riskDeV.model.documentDB.PackageVersion;
import it.unipi.riskDeV.model.graphDB.PackageVersionNode;
import it.unipi.riskDeV.repository.documentDB.PackageVersionRepository;
import it.unipi.riskDeV.repository.graphDB.PackageVersionGraphRepository;
import it.unipi.riskDeV.results.DomainError;
import it.unipi.riskDeV.results.Result;
import it.unipi.riskDeV.util.DependencyParser;
import it.unipi.riskDeV.util.Helper;
import it.unipi.riskDeV.util.VersionParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
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
    @Transactional(transactionManager = "neo4jTransactionManager")
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

    // Returns the latest version of the specified package with no known vulnerabilities, also taking transactional vulnerabilities into account.
    
    public Result<?> getIndirectSafeVersions(String packageName) {
        if (!packageVersionRepository.existsByPackageName(packageName)) {
            return new Result.Failure<>(new DomainError.NotFound("Package " + packageName + " not found."));
        }

        var safeVersions = packageVersionRepository.findByPackageNameAndRiskScoreOrderByVersionArrayDesc(packageName, 0);
        
        for (PackageVersion version : safeVersions) {
            List<VulnerabilityReportDTO> vulnerabilities = packageVersionGraphRepository.findRecursiveVulnerabilities(version.getPackageName(), version.getVersion());
            if (vulnerabilities.isEmpty()) {
                return new Result.Success<>(new PackageVersionDTO(version));
            }
        }
        return new Result.Success<>(new MessageResponseDTO("No versions found."));
    }

    // Add a new version of a package.
    @Transactional
    public Result<MessageResponseDTO> addNewVersion(String packageName, AddPackageVersionDTO newVersionDTO) {
        if (!packageName.equals(newVersionDTO.getPackageName())) {
            return new Result.Failure<>(new DomainError.InvalidOperation("The package name in the URL does not match the body"));
        }
        
        String version = newVersionDTO.getVersion();
        if (packageVersionRepository.existsByPackageNameAndVersion(packageName, version)) {
            return new Result.Failure<>(new DomainError.AlreadyExists("Version " + version + " already exists."));
        }

        PackageVersion versionDoc = new PackageVersion(newVersionDTO, versionParser);
        packageVersionRepository.save(versionDoc);

        PublishedVersionDTO publishedVersionDTO = new PublishedVersionDTO(versionDoc);
        eventPublisher.publishEvent(new PackageEvent.VersionRelease(publishedVersionDTO));
        
        return new Result.Success<>(new MessageResponseDTO("Package version created successfully"));    
    }

    // Updates the general metadata of a package for all its versions.
    @Transactional
    public Result<MessageResponseDTO> updatePackageMetadata(String packageName, UpdateGeneralPackageDTO updateData) {
        if (!packageVersionRepository.existsByPackageName(packageName)) {
            return new Result.Failure<>(new DomainError.NotFound("Package " + packageName + " not found."));
        }

        helper.updatePackageGeneralMetadata(packageName, updateData);

        updateData.getDocumentationURL().ifPresent(url -> eventPublisher.publishEvent(new PackageEvent.UpdateDocumentation(packageName, url)));
        return new Result.Success<>(new MessageResponseDTO("Update executed successfully"));
    }

    // Updates the data of a specific package version.
    // TODO: TEST!!!!
    @Transactional
    public Result<MessageResponseDTO> updatePackageVersion(String packageName, String version, UpdatePackageVersionDTO updateVersionDTO) {
        var existingOpt = packageVersionRepository.findByPackageNameAndVersion(packageName, version);
        if (existingOpt.isEmpty()) {
            return new Result.Failure<>(new DomainError.NotFound("Package " + packageName + " " + version + " not found."));
        }
        
        PackageVersion updateVersion = existingOpt.get();
        updateVersionDTO.getRequiresPython().ifPresent(updateVersion::setRequiresPython);
        updateVersion.setUploadTime(Instant.now());

        if (updateVersionDTO.getDependencies() != null) {
            List<Constraints> dependencyList = updateVersionDTO.getDependencies().stream()
                .map(DependencyParser::parseFullString)
                .toList();
            updateVersion.setDependencies(dependencyList);
        }

        if (updateVersionDTO.getVulnerabilities() != null) {
            List<EmbeddedVulnerability> vulnList = updateVersionDTO.getVulnerabilities().stream()
                .map(EmbeddedVulnerability::new) 
                .toList();
            updateVersion.setVulnerabilities(vulnList);
        }

        packageVersionRepository.save(updateVersion);

        List<ConstraintsDTO> constraintsDtos = (updateVersion.getDependencies() != null) 
            ? updateVersion.getDependencies().stream().map(ConstraintsDTO::new).toList() 
            : new ArrayList<>();
        eventPublisher.publishEvent(new PackageEvent.UpdatePackageVersion(packageName, version, constraintsDtos, updateVersionDTO.getVulnerabilities()));

        return new Result.Success<>(new MessageResponseDTO("Package updated successfully"));
    }

    // Deletes a specific version of a package.
    @Transactional
    public Result<MessageResponseDTO> deletePackageVersion(String packageName, String version) {
        if (!packageVersionRepository.existsByPackageNameAndVersion(packageName, version)) {
            return new Result.Failure<>(new DomainError.NotFound("Package version not found."));
        }

        packageVersionRepository.deleteByPackageNameAndVersion(packageName, version);

        eventPublisher.publishEvent(new PackageEvent.DeletePackageVersion(packageName, version));
        return new Result.Success<>(new MessageResponseDTO("Package deleted successfully"));
    }
}