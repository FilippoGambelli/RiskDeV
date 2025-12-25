package it.unipi.riskDeV.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import it.unipi.riskDeV.DTO.GeneralPackageDTO;
import it.unipi.riskDeV.DTO.PackageVersionDTO;
import it.unipi.riskDeV.service.PackageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/packages")
@Tag(name = "Package controller", description = "API for Python Packages operations")
public class PackageController {

    private final PackageService packageService;

    // Constructor
    public PackageController(PackageService packageService) {
        this.packageService = packageService;
    }

    // Query getPackageByName: Info package + version list
    // Example: GET /api/packages/{name}
    @GetMapping("/{packageName}")
    public ResponseEntity<GeneralPackageDTO> getPackageByName(@PathVariable String packageName) {
        System.out.println("Searching package by name: " + packageName);
        
        return packageService.getPackageByName(packageName)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Query getPackageVersion: Info specific version
    // Example: GET /api/packages/{name}/{version}
    @GetMapping("/{packageName}/{version}")
    public ResponseEntity<PackageVersionDTO> getPackageVersion(
            @PathVariable String packageName, 
            @PathVariable String version) {
            
        System.out.println("Searching package version information of: " + packageName + " " + version);

        return packageService.getPackageVersion(packageName, version)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}