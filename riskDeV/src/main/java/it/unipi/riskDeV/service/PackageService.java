package it.unipi.riskDeV.service;

import it.unipi.riskDeV.DTO.GeneralPackageDTO;
import it.unipi.riskDeV.DTO.PackageVersionDTO;
import it.unipi.riskDeV.exception.PackageNotFoundException;
import it.unipi.riskDeV.model.Package;
import it.unipi.riskDeV.model.PackageVersion;
import it.unipi.riskDeV.repository.GeneralPackageRepository;
import it.unipi.riskDeV.repository.PackageVersionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import it.unipi.riskDeV.repository.PackageVersionGraphRepository;
import it.unipi.riskDeV.model.neo4j.PackageVersionNode;
import java.util.stream.Collectors;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PackageService {

    private final GeneralPackageRepository generalPackageRepository;
    private final PackageVersionRepository packageVersionRepository;
    private final PackageVersionGraphRepository packageVersionGraphRepository;

    public GeneralPackageDTO getPackageByName(String packageName) {
        Package pkg = generalPackageRepository.findById(packageName)
                .orElseThrow(() -> new PackageNotFoundException(
                    "Package " + packageName + " not found."
                ));
        
        return new GeneralPackageDTO(pkg);
    }

    public PackageVersionDTO getPackageByNameVersion(String packageName, String packageVersion) {
        PackageVersion pkg = packageVersionRepository.findById(packageName + " " + packageVersion)
                .orElseThrow(() -> new PackageNotFoundException(
                    "Package " + packageName + " " + packageVersion + "not found."
                ));
        
        return new PackageVersionDTO(pkg);
    }

    public List<String> getPackagesDependingOn(String packageName) {
        log.info("Searching for packages depending on: {}", packageName);

        // Checking if the package exists
        if (!generalPackageRepository.existsById(packageName)) {
            throw new PackageNotFoundException("Package " + packageName + " not found.");
        }

        List<PackageVersionNode> dependents = packageVersionGraphRepository.findReverseDependencies(packageName);
        
        return dependents.stream()
                .map(PackageVersionNode::getId)
                .collect(Collectors.toList());
    }
}