package it.unipi.riskDeV.service;

import it.unipi.riskDeV.DTO.GeneralPackageDTO;
import it.unipi.riskDeV.DTO.PackageVersionDTO;
import it.unipi.riskDeV.exception.PackageNotFoundException;
import it.unipi.riskDeV.exception.ServiceException;
import it.unipi.riskDeV.model.Package;
import it.unipi.riskDeV.model.PackageVersion;
import it.unipi.riskDeV.repository.GeneralPackageRepository;
import it.unipi.riskDeV.repository.PackageVersionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import it.unipi.riskDeV.repository.PackageVersionGraphRepository;
import it.unipi.riskDeV.repository.PackageGraphRepository;
import it.unipi.riskDeV.model.neo4j.PackageVersionNode;
import it.unipi.riskDeV.model.neo4j.PackageNode;

import java.util.stream.Collectors;
import java.util.List;
import java.util.ArrayList;



@Service
@RequiredArgsConstructor
@Slf4j
public class PackageService {

    private final GeneralPackageRepository generalPackageRepository;
    private final PackageVersionRepository packageVersionRepository;
    private final PackageGraphRepository packageGraphRepository;

    // Get a package by its name
    public GeneralPackageDTO getPackageByName(String packageName) {
        Package pkg = generalPackageRepository.findById(packageName)
                .orElseThrow(() -> new PackageNotFoundException(
                    "Package " + packageName + " not found."
                ));
        
        return new GeneralPackageDTO(pkg);
    }

    // Get information about a specific version of a package 
    public PackageVersionDTO getPackageByNameVersion(String packageName, String packageVersion) {
        PackageVersion pkg = packageVersionRepository.findById(packageName + " " + packageVersion)
                .orElseThrow(() -> new PackageNotFoundException(
                    "Package " + packageName + " " + packageVersion + "not found."
                ));
        
        return new PackageVersionDTO(pkg);
    }

    // Get all the dependencies required by a package
    public List<String> getPackagesDependingOn(String packageName) {
        log.info("Searching for packages depending on: {}", packageName);

        // Checking if the package exists
        if (!packageGraphRepository.existsById(packageName)) {
            throw new PackageNotFoundException("Package " + packageName + " not found.");
        }

        List<PackageVersionNode> dependents = packageVersionGraphRepository.findReverseDependencies(packageName);
        
        return dependents.stream()
                .map(PackageVersionNode::getId)
                .collect(Collectors.toList());
    }

    // Get all the dependencies of a specific package
    public List<String> getDirectDependencies(String packageName, String version) {
        // Id is defined as packageName + ' ' + version
        String versionId = packageName + " " + version;
        log.info("Searching for direct dependencies of: {}", versionId);

        // Checking if the node exists
        if (!packageVersionGraphRepository.existsById(versionId)) {
            throw new PackageNotFoundException("Package version " + versionId + " not found in the graph.");
        }

        List<PackageVersionNode> dependencies = packageVersionGraphRepository.findDirectDependencies(versionId);

        return dependencies.stream()
                .map(PackageVersionNode::getId)
                .collect(Collectors.toList());
    }

    // Get versions without CVEs of a specific package
    public List<PackageVersionDTO> getSafeVersions(String packageName) {
        log.info("Searching for safe versions of package: {}", packageName);

        // Checking if the package exists
        if (!generalPackageRepository.existsById(packageName)) {
            throw new PackageNotFoundException("Package " + packageName + " not found.");
        }

        List<PackageVersion> safeVersions = packageVersionRepository.findSafeVersions(packageName);

        // Mapping a DTO
        return safeVersions.stream()
                .map(PackageVersionDTO::new)
                .collect(Collectors.toList());
    }

    // Add a new package
    public void addNewPackage(GeneralPackageDTO packageDTO) {
        String packageName = packageDTO.getPackageName();
        log.info("Registering new package: {}", packageName);

        // Checks for validation
        if (generalPackageRepository.existsById(packageName)) {
            throw new ServiceException("Package " + packageName + " already exists.");
        }

        // Write on MongoDB
        Package pkg = new Package();
        pkg.setId(packageName);
        pkg.setAuthor(packageDTO.getAuthor());
        pkg.setAuthorEmail(packageDTO.getAuthorEmail());
        pkg.setDescription(packageDTO.getDescription());
        pkg.setPackageURL(packageDTO.getPackageURL());
        pkg.setSummary(packageDTO.getSummary());
        pkg.setDocumentationURL(packageDTO.getDocumentationURL());
        pkg.setHomepageURL(packageDTO.getHomepageURL());
        pkg.setVersions(new ArrayList<>()); // The versions can be added with a specific query

        generalPackageRepository.save(pkg);

        // Write on Neo4j
        try {
            PackageNode packageNode = new PackageNode();
            packageNode.setId(packageName);
            
            packageGraphRepository.save(packageNode);
            
            log.info("Successfully registered package {}", packageName);

        } catch (Exception e) {  // <----------- we have to think about rollback fails
            // MongoDB rollback
            log.error("Neo4j write failed for package {}. Rolling back Mongo...", packageName, e);
            
            try {
                generalPackageRepository.deleteById(packageName);
                log.info("Rollback successful.");
            } catch (Exception rollbackEx) {
                log.error("CRITICAL: Failed to rollback package creation for {}", packageName);
            }
            
            throw new ServiceException("Failed to create package in Graph DB. Operation rolled back.");
        }
    }

    // Add a new version of an existing package
    public void addNewVersion(String packageName, PackageVersionDTO newVersionDTO) {
        String version = newVersionDTO.getVersion();
        String newId = packageName + " " + version;
        log.info("Adding new version {} to package {}", version, packageName);

        // Checks for validation
        Package pkg = generalPackageRepository.findById(packageName)
                .orElseThrow(() -> new PackageNotFoundException("Package " + packageName + " not found."));

        if (packageVersionRepository.existsById(newId)) {
            throw new ServiceException("Version " + version + " already exists.");
        }


        // --- Write on MongoDB ---

        // Creation of the document
        PackageVersion versionDoc = new PackageVersion();
        versionDoc.setId(newId);
        versionDoc.setPackageName(packageName);
        versionDoc.setVersion(version);
        versionDoc.setUploadTime(newVersionDTO.getUploadTime());
        versionDoc.setRequiresPython(newVersionDTO.getRequiresPython());
        versionDoc.setDependencies(newVersionDTO.getDependencies());
        if (newVersionDTO.getVulnerabilities() != null) {
            versionDoc.setVulnerabilities(newVersionDTO.getVulnerabilities());
        } else {
            versionDoc.setVulnerabilities(new ArrayList<>());
        }

        // Save of the document
        packageVersionRepository.save(versionDoc);

        // Update of the father (Package)
        if (pkg.getVersions() == null) pkg.setVersions(new ArrayList<>());
        pkg.getVersions().add(version);
        generalPackageRepository.save(pkg);


        // --- Write on Neo4j ---

        try {
            PackageNode packageNode = packageGraphRepository.findById(packageName)
                    .orElseThrow(() -> new RuntimeException("Graph node missing for " + packageName));

            PackageVersionNode versionNode = new PackageVersionNode();
            versionNode.setId(newId);
            versionNode.setVersion(version);
            
            // Parsing the version
            try {
                String[] parts = version.split("\\.");
                if (parts.length > 0) versionNode.setMajor(Integer.parseInt(parts[0]));
                if (parts.length > 1) versionNode.setMinor(Integer.parseInt(parts[1]));
                if (parts.length > 2) versionNode.setPatch(Integer.parseInt(parts[2]));
            } catch (Exception e) { /* ignore */ }

            // Creation of nodes and vulnerabilities in the graph
            if (newVersionDTO.getVulnerabilities() != null && !newVersionDTO.getVulnerabilities().isEmpty()) {
                versionNode.setVulnerabilities(new ArrayList<>());
                
                for (EmbeddedVulnerability dtoVuln : newVersionDTO.getVulnerabilities()) {
                    // Vulnerability node has CVE as Id
                    VulnerabilityNode vNode = new VulnerabilityNode();
                    vNode.setId(dtoVuln.getCveId());
                    
                    versionNode.getVulnerabilities().add(vNode);
                }
            }

            // Linking to the father: the Package Node
            if (packageNode.getVersions() == null) packageNode.setVersions(new ArrayList<>());
            packageNode.getVersions().add(versionNode);
            
            //Cascade saves (Package -> Version -> Vulnerabilities)
            packageGraphRepository.save(packageNode);
            log.info("Successfully published version {} with {} vulnerabilities", newId, 
                    (newVersionDTO.getVulnerabilities() != null ? newVersionDTO.getVulnerabilities().size() : 0));

        } catch (Exception e) {  // <---------------------------- we have to think how handle dramatic rollback fails
            // MongoDB manual rollback
            log.error("Neo4j write failed! Rolling back MongoDB changes...", e);
            packageVersionRepository.deleteById(newId);
            pkg.getVersions().remove(version);
            generalPackageRepository.save(pkg);
            throw new ServiceException("Failed to update Graph DB. Transaction rolled back.");
        }
    }

    // Update package's metadata
    public GeneralPackageDTO updatePackageMetadata(String packageName, GeneralPackageDTO updateData) {
        log.info("Updating metadata for package: {}", packageName);

        // Searching for the document
        Package pkg = generalPackageRepository.findById(packageName)
                .orElseThrow(() -> new PackageNotFoundException("Package " + packageName + " not found."));

        // Update of information (not id and versions, there are other queries to add/delete versions)
        pkg.setAuthor(updateData.getAuthor());
        pkg.setAuthorEmail(updateData.getAuthorEmail());
        pkg.setDescription(updateData.getDescription());
        pkg.setPackageURL(updateData.getPackageURL());
        pkg.setSummary(updateData.getSummary());
        pkg.setDocumentationURL(updateData.getDocumentationURL());
        pkg.setHomepageURL(updateData.getHomepageURL());

        // Save
        Package updatedPkg = generalPackageRepository.save(pkg);
        return new GeneralPackageDTO(updatedPkg);
    }

    // Delete a specific version of a package
    public void deletePackageVersion(String packageName, String version) {
        String versionId = packageName + " " + version;
        log.info("Deleting version {} of package {}", version, packageName);

        // Check for validation and backup for rollback
        Package pkg = generalPackageRepository.findById(packageName)
                .orElseThrow(() -> new PackageNotFoundException("Package " + packageName + " not found."));

        PackageVersion versionDoc = packageVersionRepository.findById(versionId)
                .orElseThrow(() -> new PackageNotFoundException("Version " + version + " not found."));

        // Delete on MongoDB
        packageVersionRepository.deleteById(versionId);

        // Remove version from father lists
        boolean removed = false;
        if (pkg.getVersions() != null) {
            removed = pkg.getVersions().remove(version);
            generalPackageRepository.save(pkg);
        }

        // Delete from Neo4j
        try {
            // deleteById remove node and its relationships
            if (!packageVersionGraphRepository.existsById(versionId)) {
                log.warn("Version node {} not found in Graph, but deleted from Mongo.", versionId);
            } else {
                packageVersionGraphRepository.deleteById(versionId);
            }
            log.info("Successfully deleted version {}", versionId);

        } catch (Exception e) {
            // Rollback: re-write on MongoDB what we deleted
            log.error("Neo4j delete failed! Restoring data on Mongo...", e);

            try {
                // Save again the versionDoc (backup), MongoDB uses the same id so not problems
                packageVersionRepository.save(versionDoc);

                // Link again with the father (GeneralPackage)
                if (removed) {
                    pkg.getVersions().add(version);
                    generalPackageRepository.save(pkg);
                }
                
                log.info("Rollback successful. Data restored on Mongo.");

            } catch (Exception rollbackEx) {
                // If the rollback fails, we have to see how to handle it properly --------------------------------------
                log.error("CRITICAL: Failed to rollback DELETE for {}", versionId);
                log.error("Data is now deleted from Mongo but orphaned in Neo4j.");
                throw new ServiceException("System Error: Critical data inconsistency during deletion.");
            }

            throw new ServiceException("Failed to delete from Graph DB. Operation rolled back.");
        }
    }
}