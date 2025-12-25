package it.unipi.riskDeV.service;

import it.unipi.riskDeV.DTO.GeneralPackageDTO;
import it.unipi.riskDeV.DTO.PackageVersionDTO;
import it.unipi.riskDeV.repository.GeneralPackageRepository;
import it.unipi.riskDeV.repository.PackageVersionRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PackageService {

    private final GeneralPackageRepository generalRepo;
    private final PackageVersionRepository versionRepo;

    // Constructor
    public PackageService(GeneralPackageRepository generalRepo, PackageVersionRepository versionRepo) {
        this.generalRepo = generalRepo;
        this.versionRepo = versionRepo;
    }

    // Retrieves a package by name and converts it to a DTO. Returns an empty Optional if it doesn't exist
    public Optional<GeneralPackageDTO> getPackageByName(String packageName) {
        return generalRepo.findByPackage_name(packageName)
                .map(GeneralPackageDTO::new);
    }

    // Retrieves a specific version and converts it to a DTO
    public Optional<PackageVersionDTO> getPackageVersion(String packageName, String version) {
        return versionRepo.findByPackage_nameAndVersion(packageName, version)
                .map(PackageVersionDTO::new);
    }
}