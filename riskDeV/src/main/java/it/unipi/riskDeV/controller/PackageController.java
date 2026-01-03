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
import org.springframework.http.HttpStatus;
import java.util.List;

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

    @GetMapping("/{packageName}/dependents")
    @Operation(summary = "Find reverse dependencies",
            description = "Returns a list of package versions that depend on the specified package (using Neo4j).")
    public List<String> getReverseDependencies(
            @Parameter(description = "The package name", example = "numpy") 
            @PathVariable String packageName) {
        return packageService.getPackagesDependingOn(packageName);
    }

    @GetMapping("/{packageName}/safe")
    @Operation(summary = "Get safe versions",
            description = "Returns a list of all versions of the specified package that have NO known vulnerabilities.")
    public List<PackageVersionDTO> getSafeVersions(
            @Parameter(description = "The package name", example = "django") 
            @PathVariable String packageName) {
        return packageService.getSafeVersions(packageName);
    }

    @GetMapping("/{packageName}/{packageVersion}/dependencies")
    @Operation(summary = "Get direct dependencies",
            description = "Returns a list of packages (and their versions) that the specified package version directly depends on.")
    public List<String> getDirectDependencies(
            @Parameter(description = "The package name", example = "pandas") 
            @PathVariable String packageName,
            @Parameter(description = "The package version", example = "1.3.0") 
            @PathVariable String packageVersion) {
        return packageService.getDirectDependencies(packageName, packageVersion);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a new package",
            description = "Creates a new empty package container in both MongoDB and Neo4j.")
    public void addNewPackage(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Package details")
            @RequestBody GeneralPackageDTO packageDTO) {
        
        // Input validation
        if (packageDTO.getPackageName() == null || packageDTO.getPackageName().isEmpty()) {
            throw new ServiceException("Package name is required");
        }
        
        packageService.addNewPackage(packageDTO);
    }

    @PostMapping("/{packageName}/versions")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Publish a new version",
            description = "Creates a new version document in MongoDB and updates the Neo4j graph with the new node and relationship.")
    public void addNewVersion(
            @Parameter(description = "The package name", example = "numpy") 
            @PathVariable String packageName,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "New version details")
            @RequestBody PackageVersionDTO newVersionDTO) {
        
        packageService.addNewVersion(packageName, newVersionDTO);
    }

    @PutMapping("/{packageName}")
    @Operation(summary = "Update package metadata",
            description = "Updates the descriptive information (author, email, description) of a package. Does not affect versions or graph topology.")
    public GeneralPackageDTO updatePackageMetadata(
            @Parameter(description = "The name of the package to update", example = "requests") 
            @PathVariable String packageName,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Updated package details")
            @RequestBody GeneralPackageDTO packageDTO) {
        
        return packageService.updatePackageMetadata(packageName, packageDTO);
    }

    @DeleteMapping("/{packageName}/{packageVersion}")
    @ResponseStatus(HttpStatus.NO_CONTENT) // 204 No Content (is the delete standard)
    @Operation(summary = "Unpublish a version",
            description = "Deletes a specific version of a package from both MongoDB and Neo4j. Rolls back if consistency check fails.")
    public void deletePackageVersion(
            @Parameter(description = "The package name", example = "numpy") 
            @PathVariable String packageName,
            @Parameter(description = "The version to delete", example = "1.21.0") 
            @PathVariable String packageVersion) {
        
        packageService.deletePackageVersion(packageName, packageVersion);
    }
}