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
import it.unipi.riskDeV.results.DomainError;
import it.unipi.riskDeV.results.Result;
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
    

    public Result<Void> createProjectStructure(String projectName, String adminId, List<InstalledPackageDTO> installedPackages) {
        try {
            projectGraphRepository.createProjectNode(projectName);

            for (InstalledPackageDTO installedPackage : installedPackages) {
                projectGraphRepository.addDependency(projectName, installedPackage.getName(), installedPackage.getVersion());
            }

            return new Result.Success<>(null);
        } catch (Exception e) {
            return new Result.Failure<>(new DomainError.SystemError(e));
        }
        
    }

    public Result<Void> deleteProjectNode(String projectName) {
        try {
            projectGraphRepository.deleteProjectByName(projectName);
            return new Result.Success<>(null);
        } catch (Exception e) {
            return new Result.Failure<>(new DomainError.SystemError(e));
        }
    }

    public Result<Void> syncProjectPackages(String projectName, List<InstalledPackageDTO> installedPackages) {
        try {

            projectGraphRepository.removeAllDependency(projectName);
            for (InstalledPackageDTO installedPackage : installedPackages) {
                projectGraphRepository.addDependency(projectName, installedPackage.getName(), installedPackage.getVersion());
            }

            return new Result.Success<>(null);
        } catch (Exception e) {
            return new Result.Failure<>(new DomainError.SystemError(e));
        }
    }

    public Result<Void> addVulnerability(String cveId, String description, Double baseScore) {
        VulnerabilityNode vulnerabilityNode = new VulnerabilityNode(cveId, description, baseScore);
        
        try {
            vulnerabilityGraphRepository.save(vulnerabilityNode);
        } catch (Exception e) {
            return new Result.Failure<>(new DomainError.SystemError(e));
        }
        return new Result.Success<>(null);

    }

    public Result<Void> updateVulnerability(String cveId, String description, Double baseScore) {
        VulnerabilityNode vulnerabilityNode = vulnerabilityGraphRepository.findByCveId(cveId).get();
        if(description != null) {
            vulnerabilityNode.setDescription(description);
        }
        if(baseScore != null) {
            vulnerabilityNode.setBaseScore(baseScore);
        }

        try {
            vulnerabilityGraphRepository.save(vulnerabilityNode);
        } catch (Exception e) {
            return new Result.Failure<>(new DomainError.SystemError(e));
        }
        return new Result.Success<>(null);
        
    }

    public Result<Void> deleteVulnerability(String cveId) {
        try {
            vulnerabilityGraphRepository.deleteByCveId(cveId);
        } catch (Exception e) {
            return new Result.Failure<>(new DomainError.SystemError(e));
        }
        return new Result.Success<>(null);
    }


    public Result<Void> addPackage(PublishedVersionDTO publishedVersionDTO, Double risk_score) {
        PackageVersionNode packageVersioneNode = new PackageVersionNode(publishedVersionDTO, risk_score);
        
        try {
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

            return new Result.Success<>(null);
        } catch (Exception e) {
            return new Result.Failure<>(new DomainError.SystemError(e));
        }

    }

    public Result<Void> updatePackageDocumentation(String packageName, String documentationURL) {
        try {
            packageVersionGraphRepository.updateDocumentation(packageName, documentationURL); 
            return new Result.Success<>(null);  
        } catch (Exception e) {
            return new Result.Failure<>(new DomainError.SystemError(e));
        }
    }

    public Result<Void> updatePackageVersion(String packageName, String version, List<ConstraintsDTO> dependecies, List<EmbeddedVulnerabilityDTO> vulnerabilities) {
        if(!dependecies.isEmpty()) {
            packageVersionGraphRepository.deleteDependencies(packageName, version);
            List<PackageVersion> packageVersionList = helper.addDependeciesGraph(packageName, version, dependecies);
            for (PackageVersion packageVersion: packageVersionList) {
                try {
                    packageVersionGraphRepository.attachDependency(packageName, version, packageVersion.getPackageName(), packageVersion.getVersion());
                    return new Result.Success<>(null); 
                } catch (Exception e) {
                    return new Result.Failure<>(new DomainError.SystemError(e));
                }
            }
        }

        if(!vulnerabilities.isEmpty()) {
            packageVersionGraphRepository.deleteVulnerabilities(packageName, version);

            List<EmbeddedVulnerabilityDTO> vulnerabilityList = vulnerabilities;
            for (EmbeddedVulnerabilityDTO vulnerability : vulnerabilityList) {
                try {
                    packageVersionGraphRepository.attachVulnerability(packageName, version, vulnerability.getCveId());
                } catch (Exception e) {
                    return new Result.Failure<>(new DomainError.SystemError(e));
                }
            }
        }

        return new Result.Failure<>(new DomainError.SystemError("Impossible to update package version."));
    }

    public Result<Void> deletePackageVersion(String packageName, String version) {
        try {
            packageVersionGraphRepository.deleteByPackageNameAndVersion(packageName, version);
            return new Result.Success<>(null);
        } catch (Exception e) {
            return new Result.Failure<>(new DomainError.SystemError(e));
        }
    }
}