package it.unipi.riskDeV.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.unipi.riskDeV.DTO.GeneralPackageDTO;
import it.unipi.riskDeV.DTO.PackageVersionDTO;
import it.unipi.riskDeV.service.PackageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/packages")
@Tag(name = "Package controller", description = "API for Python Packages operations")
@RequiredArgsConstructor
@Slf4j
public class PackageController {

    private final PackageService packageService;

    @GetMapping("/{packageName}")
    @Operation(summary = "List of all the generic information about a specific package",
            description = "Fetches all the generic information about a specific package. This includes: author, description, homepage url and a list of all the versions")
    public GeneralPackageDTO getPackageByName(
            @Parameter(
                description = "The name of the package to retrieve details", example = "numpy",
                required = true, schema = @Schema(type = "string")
            ) @PathVariable String packageName) {
        log.info("Searching package by name: {}", packageName);
        return packageService.getPackageByName(packageName);
    }

    @GetMapping("/{packageName}/{packageVersion}")
    @Operation(summary = "List of all the information about a specific package version",
            description = "Fetches all the information about a specific package version. This includes: upload time, requires packages, requires python version and a list of all the vulnerabilities")
    public PackageVersionDTO getPackageVersion(
            @Parameter(
                description = "The name of the package to retrieve details", example = "numpy",
                required = true, schema = @Schema(type = "string")
            ) @PathVariable String packageName, 
            @Parameter(
                description = "The version of the package to retrieve details", example = "0.9.6",
                required = true, schema = @Schema(type = "string")
            ) @PathVariable String packageVersion) {
        log.info("Searching package version information of: {} {}", packageName, packageVersion);
        return packageService.getPackageByNameVersion(packageName, packageVersion);
    }
}