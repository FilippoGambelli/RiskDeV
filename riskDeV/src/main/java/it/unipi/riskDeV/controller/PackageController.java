package it.unipi.riskDeV.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.unipi.riskDeV.DTO.ErrorResponseDTO;
import it.unipi.riskDeV.DTO.packageVersion.AddPackageVersionDTO;
import it.unipi.riskDeV.DTO.packageVersion.UpdateGeneralPackageDTO;
import it.unipi.riskDeV.DTO.packageVersion.PackageVersionDTO;
import it.unipi.riskDeV.DTO.packageVersion.ReverseDependencyDTO;
import it.unipi.riskDeV.DTO.packageVersion.UpdatePackageVersionDTO;
import it.unipi.riskDeV.results.RestResponseMapper;
import it.unipi.riskDeV.service.PackageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api/packages")
@Tag(name = "Package Controller", description = "APIs for managing Python packages, including versioning, dependencies, and vulnerabilities")
@RequiredArgsConstructor
@ApiResponses(value = {
    @ApiResponse(
        responseCode = "500",
        description = "Internal Server Error",
        content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
    )
})
public class PackageController {

    private final PackageService packageService;
    private final RestResponseMapper restResponseMapper;

    @GetMapping("/{packageName}")
    @Operation(
        summary = "Get latest package information",
        description = "Returns all information about the latest version of the specified package, including metadata, dependencies and vulnerabilities."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Package found", content = @Content(schema = @Schema(implementation = PackageVersionDTO.class))),
        @ApiResponse(responseCode = "404", description = "Package not found", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<?> getPackageByName(@Parameter(description = "Name of the package", required = true, example = "Django") @PathVariable String packageName) {
        return restResponseMapper.map(packageService.getPackageByName(packageName), HttpStatus.OK);
    }

    @GetMapping("/{packageName}/{version}")
    @Operation(
        summary = "Get package version information",
        description = "Returns all information about a specific version of a package, including metadata, dependencies and vulnerabilities."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Package version found", content = @Content(schema = @Schema(implementation = PackageVersionDTO.class))),
        @ApiResponse(responseCode = "404", description = "Package version not found", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<?> getPackageVersion(
            @Parameter(description = "Package name", required = true, example = "numpy") @PathVariable String packageName,
            @Parameter(description = "Package version", required = true, example = "1.21.0") @PathVariable String version) {
        return restResponseMapper.map(packageService.getPackageByNameVersion(packageName, version), HttpStatus.OK);
    }

    @GetMapping("/{packageName}/{version}/direct-dependencies")
    @Operation(
        summary = "Get direct dependencies",
        description = "Returns a list of direct dependency for a specific package version"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Dependencies found", content = @Content(array = @ArraySchema(schema = @Schema(implementation = String.class)))),
        @ApiResponse(responseCode = "404", description = "Package version not found", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<?> getDirectDependencies(
            @Parameter(description = "Package name", required = true, example = "pandas") @PathVariable String packageName,
            @Parameter(description = "Package version", required = true, example = "1.3.0") @PathVariable String version) {
        return restResponseMapper.map(packageService.getDirectDependencies(packageName, version), HttpStatus.OK);
    }

    @GetMapping("/{packageName}/{version}/reverse-dependencies")
    @Operation(
        summary = "Get reverse dependencies",
        description = "Returns a list of package versions that depend on the specified package version."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Reverse dependencies found", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ReverseDependencyDTO.class)))),
        @ApiResponse(responseCode = "404", description = "Package version not found", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<?> getReverseDependencies(
            @Parameter(description = "Package name", required = true, example = "numpy") @PathVariable String packageName,
            @Parameter(description = "Package version", required = true, example = "0.9.6") @PathVariable String version) {
        return restResponseMapper.map(packageService.getPackagesDependingOn(packageName, version), HttpStatus.OK);
    }

    @GetMapping("/{packageName}/safe")
    @Operation(
        summary = "Get safe versions",
        description = "Returns the last version of the specified package with no known vulnerabilities."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Safe versions found", content = @Content(array = @ArraySchema(schema = @Schema(implementation = PackageVersionDTO.class)))),
        @ApiResponse(responseCode = "404", description = "Package not found", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<?> getSafeVersions(@Parameter(description = "Package name", required = true, example = "Django") @PathVariable String packageName) {
        return restResponseMapper.map(packageService.getSafeVersions(packageName), HttpStatus.OK);
    }

    @PostMapping("/{packageName}/version")
    @Operation(
        summary = "Publish a new version",
        description = "Creates a new version of a package in the database and updates graph relationships."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Version published successfully"),
        @ApiResponse(responseCode = "409", description = "Version already exists", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid version format", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<?> addNewVersion(@Parameter(description = "Package name", required = true, example = "numpy") @PathVariable String packageName,
                                           @Valid @RequestBody AddPackageVersionDTO newVersionDTO) {
        return restResponseMapper.map(packageService.addNewVersion(packageName, newVersionDTO), HttpStatus.CREATED);
    }

    @PutMapping("/{packageName}")
    @Operation(
        summary = "Update package metadata",
        description = "Updates metadata for all versions of the specified package."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Package metadata updated"),
        @ApiResponse(responseCode = "404", description = "Package not found", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<?> updatePackageMetadata(@Parameter(description = "Package name", required = true, example = "requests") @PathVariable String packageName,
                                                   @Valid @RequestBody UpdateGeneralPackageDTO packageDTO) {
        return restResponseMapper.map(packageService.updatePackageMetadata(packageName, packageDTO), HttpStatus.OK);
    }

    @PutMapping("/{packageName}/{version}")
    @Operation(
        summary = "Update package version info",
        description = "Updates details of a specific version, such as dependencies, vulnerabilities, and Python requirement."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Package version updated"),
        @ApiResponse(responseCode = "404", description = "Package version not found", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<?> updatePackageVersion(@Parameter(description = "Package name", required = true, example = "Flask") @PathVariable String packageName,
                                                  @Parameter(description = "Package version", required = true, example = "2.1.1") @PathVariable String  version,
                                                  @Valid @RequestBody UpdatePackageVersionDTO updateDTO) {
        return restResponseMapper.map(packageService.updatePackageVersion(packageName, version, updateDTO), HttpStatus.OK);
    }

    @DeleteMapping("/{packageName}/{version}")
    @Operation(
        summary = "Delete a package version",
        description = "Deletes a specific version from the database and graph."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Version deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Package version not found", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<?> deletePackageVersion(@Parameter(description = "Package name", required = true, example = "numpy") @PathVariable String packageName,
                                                  @Parameter(description = "Package version", required = true, example = "1.21.0") @PathVariable String version) {
        return restResponseMapper.map(packageService.deletePackageVersion(packageName, version), HttpStatus.NO_CONTENT);
    }
}