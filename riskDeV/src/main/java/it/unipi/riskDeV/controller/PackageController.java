package it.unipi.riskDeV.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.unipi.riskDeV.DTO.GeneralPackageDTO;
import it.unipi.riskDeV.DTO.PackageVersionDTO;
import it.unipi.riskDeV.service.PackageService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/packages")
@Tag(name = "Package controller", description = "API for Python Packages operations")
public class PackageController {

    private final PackageService packageService;

    // Constructor
    public PackageController(PackageService packageService) {
        this.packageService = packageService;
    }

    @GetMapping("/{packageName}")
    @Operation(summary = "List of all the generic information about a specific package",
            description = "Fetches all the generic information about a specific package. This includes: author, description, homepage url and a list of all the versions")
    public ResponseEntity<GeneralPackageDTO> getPackageByName(
            @Parameter(
                description = "The name of the package to retrive details", example = "numpy",
                required = true, schema = @Schema(type = "string")
            ) @PathVariable String packageName) {
        System.out.println("Searching package by name: " + packageName);
        return packageService.getPackageByName(packageName)
            .map(ResponseEntity::ok)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Package not found: " + packageName
            ));
    }

    @GetMapping("/{packageName}/{packageVersion}")
    @Operation(summary = "List of all the information about a specific package version",
            description = "Fetches all the information about a specific package version. This includes: upload time, requires packages, requires python version and a list of all the vulnerabilities")
    public ResponseEntity<PackageVersionDTO> getPackageVersion(
            @Parameter(
                description = "The name of the package to retrive details", example = "numpy",
                required = true, schema = @Schema(type = "string")
            ) @PathVariable String packageName, 
            @Parameter(
                description = "The version of the package to retrive details", example = "0.9.6",
                required = true, schema = @Schema(type = "string")
            ) @PathVariable String packageVersion) {
        System.out.println("Searching package version information of: " + packageName + " " + packageVersion);
        return packageService.getPackageVersion(packageName, packageVersion)
            .map(ResponseEntity::ok)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Package not found: " + packageName + " " + packageVersion
        ));
    }
}