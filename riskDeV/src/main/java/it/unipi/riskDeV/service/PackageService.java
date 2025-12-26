package it.unipi.riskDeV.service;

import it.unipi.riskDeV.DTO.GeneralPackageDTO;
import it.unipi.riskDeV.DTO.PackageVersionDTO;
import it.unipi.riskDeV.repository.GeneralPackageRepository;
import it.unipi.riskDeV.repository.PackageVersionRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PackageService {

    private final GeneralPackageRepository generalPackageRepository;
    private final PackageVersionRepository packageVersionRepository;

    // Constructor
    public PackageService(GeneralPackageRepository generalRepo, PackageVersionRepository versionRepo) {
        this.generalPackageRepository = generalRepo;
        this.packageVersionRepository = versionRepo;
    }

    /**
     * Retives a package by its name and converts it to a DTO.
     * 
     * @param packageName The name of the package to retrive.
     * @return An Optional containing the GeneralPackageDTO if found or empty if the package does not exist.
     */
    public Optional<GeneralPackageDTO> getPackageByName(String packageName) {
        return generalPackageRepository.findById(packageName)
                .map(GeneralPackageDTO::new);
    }

    /**
     * Retrieves a specific version of a package and converts it to a DTO.
     *
     * @param packageName The name of the package.
     * @param version The version string of the package to retrieve.
     * @return An Optional containing the PackageVersionDTO if found or empty if the version does not exist.
     */
    public Optional<PackageVersionDTO> getPackageVersion(String packageName, String version) {
        return packageVersionRepository.findById(packageName + " " + version)
                .map(PackageVersionDTO::new);
    }
}