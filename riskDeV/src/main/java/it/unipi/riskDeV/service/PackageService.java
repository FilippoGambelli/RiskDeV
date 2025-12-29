package it.unipi.riskDeV.service;

import it.unipi.riskDeV.DTO.GeneralPackageDTO;
import it.unipi.riskDeV.DTO.PackageVersionDTO;
import it.unipi.riskDeV.exception.PackageNotFoundExeption;
import it.unipi.riskDeV.model.Package;
import it.unipi.riskDeV.model.PackageVersion;
import it.unipi.riskDeV.repository.GeneralPackageRepository;
import it.unipi.riskDeV.repository.PackageVersionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PackageService {

    private final GeneralPackageRepository generalPackageRepository;
    private final PackageVersionRepository packageVersionRepository;

    public GeneralPackageDTO getPackageByName(String packageName) {
        Package pkg = generalPackageRepository.findById(packageName)
                .orElseThrow(() -> new PackageNotFoundExeption(
                    "Package " + packageName + " not found."
                ));
        
        return new GeneralPackageDTO(pkg);
    }

    public PackageVersionDTO getPackageByNameVersion(String packageName, String packageVersion) {
        PackageVersion pkg = packageVersionRepository.findById(packageName + " " + packageVersion)
                .orElseThrow(() -> new PackageNotFoundExeption(
                    "Package " + packageName + " " + packageVersion + "not found."
                ));
        
        return new PackageVersionDTO(pkg);
    }
}