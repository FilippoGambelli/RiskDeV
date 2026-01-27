package it.unipi.riskDeV.async;

import it.unipi.riskDeV.DTO.packageVersion.ConstraintsDTO;
import it.unipi.riskDeV.DTO.packageVersion.EmbeddedVulnerabilityDTO;
import it.unipi.riskDeV.DTO.packageVersion.PublishedVersionDTO;
import it.unipi.riskDeV.model.documentDB.PackageVersion;
import it.unipi.riskDeV.model.graphDB.PackageVersionNode;
import it.unipi.riskDeV.model.graphDB.VulnerabilityNode;
import it.unipi.riskDeV.repository.graphDB.PackageGraphRepository;
import it.unipi.riskDeV.repository.graphDB.PackageVersionGraphRepository;
import it.unipi.riskDeV.repository.graphDB.ProjectGraphRepository;
import it.unipi.riskDeV.repository.graphDB.VulnerabilityGraphRepository;
import it.unipi.riskDeV.util.Helper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class GraphService {

    private final ProjectGraphRepository projectGraphRepository;
    private final VulnerabilityGraphRepository vulnerabilityGraphRepository;
    private final PackageVersionGraphRepository packageVersionGraphRepository;
    private final PackageGraphRepository packageGraphRepository;
    private final Helper helper;
    

    public void createProjectStructure(String projectName, String adminId, List<String> packageIds) {
        projectGraphRepository.createProjectNode(projectName);
    
        if (adminId != null) {
            projectGraphRepository.setProjectOwner(projectName, adminId);
        }

        if (packageIds != null && !packageIds.isEmpty()) {
            projectGraphRepository.replaceAllDependencies(projectName, packageIds);
        }
    }

    public void deleteProjectNode(String projectName) {
        projectGraphRepository.deleteProjectByName(projectName);
    }

    public void syncProjectPackages(String projectName, List<String> packageIds) {
        projectGraphRepository.replaceAllDependencies(projectName, packageIds);
    }

    public void addCollaborator(String projectName, String userId) {
        projectGraphRepository.addCollaboratorRelation(projectName, userId);
    }

    public void removeCollaborator(String projectName, String userId) {
        projectGraphRepository.removeCollaboratorRelation(projectName, userId);
    }

    public void addVulnerability(String cveId, String description, Double baseScore) {
        VulnerabilityNode vulnerabilityNode = new VulnerabilityNode(cveId, description, baseScore);
        vulnerabilityGraphRepository.save(vulnerabilityNode);
    }

    public void updateVulnerability(String cveId, String description, Double baseScore) {
        VulnerabilityNode vulnerabilityNode = vulnerabilityGraphRepository.findByCveId(cveId).get();
        if(description != null) {
            vulnerabilityNode.setDescription(description);
        }
        if(baseScore != null) {
            vulnerabilityNode.setBaseScore(baseScore);
        }
        vulnerabilityGraphRepository.save(vulnerabilityNode);
    }

    public void deleteVulnerability(String cveId) {
        vulnerabilityGraphRepository.deleteByCveId(cveId);
    }


    public void addPackage(PublishedVersionDTO publishedVersionDTO, Double risk_score) {
        PackageVersionNode packageVersioneNode = new PackageVersionNode(publishedVersionDTO, risk_score);
        
        packageVersionGraphRepository.save(packageVersioneNode);
        
        packageGraphRepository.addVersionToPackage(publishedVersionDTO.getPackageName(), publishedVersionDTO.getVersion());
        
        List<String> vulnerabilityList = publishedVersionDTO.getVulnerabilities();
        for (String cveId : vulnerabilityList) {
            // TODO: We should check if the vulnerability exists, and if it doesn't, make an API request            
            packageVersionGraphRepository.attachVulnerability(publishedVersionDTO.getPackageName(), publishedVersionDTO.getVersion(), cveId);
        }

        List<ConstraintsDTO> dependecesList = publishedVersionDTO.getDependencies();
        
        List<PackageVersion> packageVersionList = helper.addDependeciesGraph(publishedVersionDTO.getPackageName(), publishedVersionDTO.getVersion(), dependecesList);
        for (PackageVersion packageVersion: packageVersionList) {
            packageVersionGraphRepository.attachDependency(publishedVersionDTO.getPackageName(), publishedVersionDTO.getVersion(), packageVersion.getPackageName(), packageVersion.getVersion());
        }

        List<PackageVersion> reversePackageVersionList = helper.updateDependeciesGraph(publishedVersionDTO.getPackageName(), publishedVersionDTO.getVersion());
        for(PackageVersion packageVersion : reversePackageVersionList) {
            packageVersionGraphRepository.attachDependency(packageVersion.getPackageName(), packageVersion.getVersion(), publishedVersionDTO.getPackageName(), publishedVersionDTO.getVersion());
        }
    }

    public void updatePackageDocumentation(String packageName, String documentationURL) {
        packageVersionGraphRepository.updateDocumentation(packageName, documentationURL);
    }

    public void updatePackageVersion(String packageName, String version, List<ConstraintsDTO> dependecies, List<EmbeddedVulnerabilityDTO> vulnerabilities) {
        if(!dependecies.isEmpty()) {
            packageVersionGraphRepository.deleteDependencies(packageName, version);
            List<PackageVersion> packageVersionList = helper.addDependeciesGraph(packageName, version, dependecies);
            for (PackageVersion packageVersion: packageVersionList) {
                packageVersionGraphRepository.attachDependency(packageName, version, packageVersion.getPackageName(), packageVersion.getVersion());
            }
        }

        if(!vulnerabilities.isEmpty()) {
            packageVersionGraphRepository.deleteVulnerabilities(packageName, version);

            List<EmbeddedVulnerabilityDTO> vulnerabilityList = vulnerabilities;
            for (EmbeddedVulnerabilityDTO vulnerability : vulnerabilityList) {
                packageVersionGraphRepository.attachVulnerability(packageName, version, vulnerability.getCveId());
            }
        }
    }

    public void deletePackageVersion(String packageName, String version) {
        packageVersionGraphRepository.deleteByPackageNameAndVersion(packageName, version);
    }
}