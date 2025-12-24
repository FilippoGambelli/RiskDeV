package it.unipi.riskDeV.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import it.unipi.riskDeV.DTO.GeneralPackageDTO;
import it.unipi.riskDeV.DTO.PackageVersionDTO;
import it.unipi.riskDeV.repository.GeneralPackageRepository;
import it.unipi.riskDeV.repository.PackageVersionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/packages")
@Tag(name = "Package controller", description = "API for Python Packages operations")
public class PackageController {

    private final GeneralPackageRepository generalRepo;
    private final PackageVersionRepository versionRepo;

    // Constructor Injection
    public PackageController(GeneralPackageRepository generalRepo, PackageVersionRepository versionRepo) {
        this.generalRepo = generalRepo;
        this.versionRepo = versionRepo;
    }

    // Query getPackageByName: Info about a package with its version list
    // Example: GET /api/packages/{name}
    @GetMapping("/{packageName}")
    public ResponseEntity<GeneralPackageDTO> getPackageByName(@PathVariable String packageName) {
        System.out.println("Searching package by name: " + packageName);
        return generalRepo.findByPackage_name(packageName)
            // Using DTO constructor
            .map(pkg -> ResponseEntity.ok(new GeneralPackageDTO(pkg)))
            .orElse(ResponseEntity.notFound().build());
    }

    // Query getPackageVersion: Info about a specific version
    // Example: GET /api/packages/{name}/{version}
    @GetMapping("/{packageName}/{version}")
    public ResponseEntity<PackageVersionDTO> getPackageVersion(
            @PathVariable String packageName, 
            @PathVariable String version) {
        System.out.println("Searching package version information of: " + packageName + " " + version);
        return versionRepo.findByPackage_nameAndVersion(packageName, version)
            // Using DTO constructor
            .map(ver -> ResponseEntity.ok(new PackageVersionDTO(ver)))
            .orElse(ResponseEntity.notFound().build());
    }

}