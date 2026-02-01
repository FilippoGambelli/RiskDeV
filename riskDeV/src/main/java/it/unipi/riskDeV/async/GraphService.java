package it.unipi.riskDeV.async;

import it.unipi.riskDeV.DTO.packageVersion.ConstraintsDTO;
import it.unipi.riskDeV.DTO.packageVersion.EmbeddedVulnerabilityDTO;
import it.unipi.riskDeV.DTO.packageVersion.PublishedVersionDTO;
import it.unipi.riskDeV.DTO.project.InstalledPackageDTO;
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
import java.util.NoSuchElementException;

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
    

    public void createProjectStructure(String projectName, List<InstalledPackageDTO> installedPackages) {
        projectGraphRepository.createProjectNode(projectName);

        for (InstalledPackageDTO installedPackage : installedPackages) {
            projectGraphRepository.addDependency(projectName, installedPackage.getName(), installedPackage.getVersion());
        }
    }

    public void deleteProjectNode(String projectName) {
        projectGraphRepository.deleteProjectByName(projectName);
    }

    public void syncProjectPackages(String projectName, List<InstalledPackageDTO> installedPackages) {
        projectGraphRepository.removeAllDependency(projectName);
        
        for (InstalledPackageDTO installedPackage : installedPackages) {
            projectGraphRepository.addDependency(projectName, installedPackage.getName(), installedPackage.getVersion());
        }
    }

    public void addVulnerability(String cveId, String description, Double baseScore) {
        VulnerabilityNode vulnerabilityNode = new VulnerabilityNode(cveId, description, baseScore);
        vulnerabilityGraphRepository.save(vulnerabilityNode);
    }

    public void updateVulnerability(String cveId, String description, Double baseScore) {
        VulnerabilityNode vulnerabilityNode = vulnerabilityGraphRepository.findByCveId(cveId)
                .orElseThrow(() -> new NoSuchElementException("Vulnerability node not found for CVE: " + cveId));
        
        if (description != null) {
            vulnerabilityNode.setDescription(description);
        }
        if (baseScore != null) {
            vulnerabilityNode.setBaseScore(baseScore);
        }

        vulnerabilityGraphRepository.save(vulnerabilityNode);
    }

    public void deleteVulnerability(String cveId) {
        vulnerabilityGraphRepository.deleteByCveId(cveId);
    }

    public void addPackage(PublishedVersionDTO publishedVersionDTO, Double riskScore) {

        PackageVersionNode packageVersionNode = new PackageVersionNode(publishedVersionDTO, riskScore);
        packageVersionGraphRepository.save(packageVersionNode);
    
        packageGraphRepository.addVersionToPackage(publishedVersionDTO.getPackageName(), publishedVersionDTO.getVersion());
        
        List<String> vulnerabilityList = publishedVersionDTO.getVulnerabilities();
        if (vulnerabilityList != null) {
            for (String cveId : vulnerabilityList) {
                // TODO: We should check if the vulnerability exists, and if it doesn't, make an API request to take it
                packageVersionGraphRepository.attachVulnerability(publishedVersionDTO.getPackageName(), publishedVersionDTO.getVersion(), cveId);
            }
        }

        List<ConstraintsDTO> constraints = publishedVersionDTO.getDependencies();
        List<PackageVersion> dependencies = helper.addDependeciesGraph(publishedVersionDTO.getPackageName(), publishedVersionDTO.getVersion(), constraints);
        
        for (PackageVersion dep : dependencies) {
            packageVersionGraphRepository.attachDependency(
                publishedVersionDTO.getPackageName(), publishedVersionDTO.getVersion(), 
                dep.getPackageName(), dep.getVersion()
            );
        }

        List<PackageVersion> reverseDeps = helper.updateDependeciesGraph(publishedVersionDTO.getPackageName(), publishedVersionDTO.getVersion());
        
        for (PackageVersion rev : reverseDeps) {
            packageVersionGraphRepository.attachDependency(
                rev.getPackageName(), rev.getVersion(), 
                publishedVersionDTO.getPackageName(), publishedVersionDTO.getVersion()
            );
        }
    }

    public void updatePackageDocumentation(String packageName, String documentationURL) {
        packageVersionGraphRepository.updateDocumentation(packageName, documentationURL);
    }

    public void updatePackageVersion(String packageName, String version, List<ConstraintsDTO> dependencies, List<EmbeddedVulnerabilityDTO> vulnerabilities) {
        
        if (dependencies != null && !dependencies.isEmpty()) {
            packageVersionGraphRepository.deleteDependencies(packageName, version);
            
            List<PackageVersion> packageVersionList = helper.addDependeciesGraph(packageName, version, dependencies);
            for (PackageVersion dep : packageVersionList) {
                packageVersionGraphRepository.attachDependency(packageName, version, dep.getPackageName(), dep.getVersion());
            }
        }

        if (vulnerabilities != null && !vulnerabilities.isEmpty()) {
            packageVersionGraphRepository.deleteVulnerabilities(packageName, version);

            for (EmbeddedVulnerabilityDTO vuln : vulnerabilities) {
                packageVersionGraphRepository.attachVulnerability(packageName, version, vuln.getCveId());
            }
        }
    }

    public void deletePackageVersion(String packageName, String version) {
        packageVersionGraphRepository.deleteByPackageNameAndVersion(packageName, version);
    }
}