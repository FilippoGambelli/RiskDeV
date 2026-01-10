package it.unipi.riskDeV.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.unipi.riskDeV.DTO.ErrorResponseDTO;
import it.unipi.riskDeV.DTO.GeneralPackageDTO;
import it.unipi.riskDeV.DTO.PackageVersionDTO;
import it.unipi.riskDeV.controller.util.RestResponseMapper;
import it.unipi.riskDeV.service.PackageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

@RestController
@RequestMapping("/api/packages")
@Tag(name = "Package controller", description = "API for Python Packages operations")
@RequiredArgsConstructor
@Slf4j
@ApiResponses(value = {
    @ApiResponse(responseCode = "500", description = "Internal System Error",
        content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
})
public class PackageController {

    private final PackageService packageService;
    private final RestResponseMapper restResponseMapper;

    @GetMapping("/{packageName}")
    @Operation(summary = "List of all the generic information about a specific package",
            description = "Fetches all the generic information about a specific package. This includes: author, description, homepage url and a list of all the versions")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Success", content = @Content(schema = @Schema(implementation = GeneralPackageDTO.class))),
        @ApiResponse(responseCode = "404", description = "Package Not Found", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })        
    public ResponseEntity<?> getPackageByName(
            @Parameter(
                description = "The name of the package to retrieve details", example = "numpy",
                required = true, schema = @Schema(type = "string")
            ) @PathVariable String packageName) {

        log.info("Searching package by name: {}", packageName);
        return restResponseMapper.map(packageService.getPackageByName(packageName), HttpStatus.OK);
    }

    @GetMapping("/{packageName}/{packageVersion}")
    @Operation(summary = "List of all the information about a specific package version",
            description = "Fetches all the information about a specific package version. This includes: upload time, requires packages, requires python version and a list of all the vulnerabilities")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Success", content = @Content(schema = @Schema(implementation = PackageVersionDTO.class))),
        @ApiResponse(responseCode = "404", description = "Package Version Not Found", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })        
    public ResponseEntity<?> getPackageVersion(
            @Parameter(
                description = "The name of the package to retrieve details", example = "numpy",
                required = true, schema = @Schema(type = "string")
            ) @PathVariable String packageName, 
            @Parameter(
                description = "The version of the package to retrieve details", example = "0.9.6",
                required = true, schema = @Schema(type = "string")
            ) @PathVariable String packageVersion) {

        log.info("Searching package version information of: {} {}", packageName, packageVersion);
        return restResponseMapper.map(packageService.getPackageByNameVersion(packageName, packageVersion), HttpStatus.OK);
    }

    @GetMapping("/{packageName}/dependents")
    @Operation(summary = "Find reverse dependencies",
            description = "Returns a list of package versions that depend on the specified package.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Success", content = @Content(schema = @Schema(implementation = List.class))),
        @ApiResponse(responseCode = "404", description = "Package Not Found", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<?> getReverseDependencies(
            @Parameter(description = "The package name", example = "numpy") 
            @PathVariable String packageName) {

        log.info("Searching reverse dependencies.");
        return restResponseMapper.map(packageService.getPackagesDependingOn(packageName), HttpStatus.OK);
    }

    @GetMapping("/{packageName}/safe")
    @Operation(summary = "Get safe versions",
            description = "Returns a list of all versions of the specified package that have NO known vulnerabilities.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Success", content = @Content(schema = @Schema(implementation = List.class))),
        @ApiResponse(responseCode = "404", description = "Package Not Found", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<?> getSafeVersions(
            @Parameter(description = "The package name", example = "django") 
            @PathVariable String packageName) {
        
        log.info("Searching safe version of the package " + packageName);
        return restResponseMapper.map(packageService.getSafeVersions(packageName), HttpStatus.OK);
    }

    @GetMapping("/{packageName}/{packageVersion}/dependencies")
    @Operation(summary = "Get direct dependencies",
            description = "Returns a list of packages (and their versions) that the specified package version directly depends on.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Success", content = @Content(schema = @Schema(implementation = List.class))),
        @ApiResponse(responseCode = "404", description = "Version Not Found", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<?> getDirectDependencies(
            @Parameter(description = "The package name", example = "pandas") 
            @PathVariable String packageName,
            @Parameter(description = "The package version", example = "1.3.0") 
            @PathVariable String packageVersion) {
        
        return restResponseMapper.map(packageService.getDirectDependencies(packageName, packageVersion), HttpStatus.OK);
    }

    @PostMapping
    @Operation(summary = "Register a new package",
            description = "Creates a new empty package container in both MongoDB and Neo4j.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Created Successfully"),
        @ApiResponse(responseCode = "409", description = "Package Already Exists", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
    })
    public ResponseEntity<?> addNewPackage(@Valid @RequestBody GeneralPackageDTO packageDTO) {
        
        return restResponseMapper.map(packageService.addNewPackage(packageDTO), HttpStatus.CREATED);
    }

    @PostMapping("/{packageName}/versions")
    @Operation(summary = "Publish a new version",
            description = "Creates a new version document in MongoDB and updates the Neo4j graph with the new node and relationship.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Version Published"),
        @ApiResponse(responseCode = "404", description = "Package Not Found", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "409", description = "Version Already Exists", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid Version Format", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<?> addNewVersion(
            @Parameter(description = "The package name", example = "numpy") 
            @PathVariable String packageName,
            @Valid @RequestBody PackageVersionDTO newVersionDTO) {
        
        return restResponseMapper.map(packageService.addNewVersion(packageName, newVersionDTO), HttpStatus.CREATED);
    }

    @PutMapping("/{packageName}")
    @Operation(summary = "Update package metadata",
            description = "Updates the descriptive information (author, email, description) of a package. Does not affect versions or graph topology.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Updated", content = @Content(schema = @Schema(implementation = GeneralPackageDTO.class))),
        @ApiResponse(responseCode = "404", description = "Package Not Found", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })        
    public ResponseEntity<?> updatePackageMetadata(
            @Parameter(description = "The name of the package to update", example = "requests") 
            @PathVariable String packageName,
            @Valid @RequestBody GeneralPackageDTO packageDTO) {
        
        return restResponseMapper.map(packageService.updatePackageMetadata(packageName, packageDTO), HttpStatus.OK);
    }

    @DeleteMapping("/{packageName}/{packageVersion}")
    @Operation(summary = "Unpublish a version",
            description = "Deletes a specific version of a package from both MongoDB and Neo4j. Rolls back if consistency check fails.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Deleted Successfully"),
        @ApiResponse(responseCode = "404", description = "Not Found", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
    })
    public ResponseEntity<?> deletePackageVersion(
            @Parameter(description = "The package name", example = "numpy") 
            @PathVariable String packageName,
            @Parameter(description = "The version to delete", example = "1.21.0") 
            @PathVariable String packageVersion) {
        
        return restResponseMapper.map(packageService.deletePackageVersion(packageName, packageVersion), HttpStatus.NO_CONTENT);
    }
    
}