package it.unipi.riskDeV.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.unipi.riskDeV.DTO.ErrorResponseDTO;
import it.unipi.riskDeV.DTO.MessageResponseDTO;
import it.unipi.riskDeV.DTO.admin.AggreagationPackageDTO;
import it.unipi.riskDeV.DTO.admin.CentralityResultDTO;
import it.unipi.riskDeV.DTO.admin.ContributorCountDTO;
import it.unipi.riskDeV.DTO.admin.PerfectStormVulnerabilityDTO;
import it.unipi.riskDeV.DTO.admin.RiskAggregationDTO;
import it.unipi.riskDeV.results.RestResponseMapper;
import it.unipi.riskDeV.service.AdminService;
import it.unipi.riskDeV.util.ResultExecutor;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/api/admin")
@SecurityRequirement(name = "bearerAuth")
@Tag(
    name = "Administrator Controller",
    description = "APIs for managing administrator actions and retrieving package centrality information"
)
@RequiredArgsConstructor
@ApiResponses(value = {
    @ApiResponse(
        responseCode = "500",
        description = "Internal server error",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class))
    )
})
public class AdminController {

    private final AdminService adminService;
    private final RestResponseMapper restResponseMapper;

    @GetMapping("/packagesWithHighCentrality")
    @Operation(
        summary = "Get the top packages by number of direct dependents",
        description = "Returns the list of packages that are depended on by the largest number of other packages (Degree Centrality). This helps identify core packages in the ecosystem."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved the top packages by degree centrality", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CentralityResultDTO.class)))    
    })
    public ResponseEntity<?> getTopByDegree() {
        return restResponseMapper.map(ResultExecutor.execute(() -> (adminService.getTopByDegree())), HttpStatus.OK);
    }

    @GetMapping("/packagesWithPageRank/{limit}")
    @Operation(
        summary = "Get the top packages by PageRank",
        description = "Returns the list of packages ranked by PageRank score, representing their global influence across the dependency network. Packages with higher scores impact a larger portion of the ecosystem."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved the top packages by PageRank", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CentralityResultDTO.class)))
    })
    public ResponseEntity<?> getTopByPageRank(
        @Parameter(description = "Limit the number of results", example = "10", required = true) @PathVariable Integer limit
    ) {
        return restResponseMapper.map(ResultExecutor.execute(() -> (adminService.getTopByPageRank(limit))), HttpStatus.OK);
    }


    @PutMapping("addNewAdmin/{username}")
    @Operation(
        summary = "Add a new administrator",
        description = "Promotes an existing user to have administrator privileges."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User successfully promoted to administrator", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "User not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<?> addNewAdmin(
        @Parameter(description = "The username of the user to be promoted to administrator", example = "francesca.romano", schema = @Schema(type = "string")) @PathVariable String username
    ) {
        return restResponseMapper.map(ResultExecutor.execute(() -> (adminService.addNewAdmin(username))), HttpStatus.OK);
    }

    @DeleteMapping("removeAdmin/{username}")
    @Operation(
        summary = "Remove an administrator",
        description = "Revokes administrator privileges from an existing administrator."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Administrator successfully removed", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Administrator not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<?> removeAdmin(
        @Parameter(description = "The username of the administrator to be removed", example = "francesca.romano", schema = @Schema(type = "string")) @PathVariable String username
    ) {
        return restResponseMapper.map(ResultExecutor.execute(() -> (adminService.removeAdmin(username))), HttpStatus.OK);
    }

    @GetMapping("/mostUsedPackages/{limit}")
    @Operation(
        summary = "Get the most used packages",
        description = "Returns the list of packages that are used as dependencies by the highest number of other packages across the entire ecosystem. This helps identify widely adopted and critical packages."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved the most used packages", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AggreagationPackageDTO.class)))
    })
    public ResponseEntity<?> getMostUsedPackages(
        @Parameter(description = "Limit the number of results", example = "10", required = true) @PathVariable Integer limit
    ) {
        return restResponseMapper.map(ResultExecutor.execute(() -> (adminService.getMostUsedPackages(limit))), HttpStatus.OK);
    }


    @GetMapping("/mostUsedPackagesLastMonth/{limit}")
    @Operation(
        summary = "Get the most used packages in the last month",
        description = "Returns the list of packages that were most frequently used as dependencies during the last month, highlighting recent trends in package adoption."
    )
    @ApiResponses({ 
        @ApiResponse(responseCode = "200", description = "Successfully retrieved the most used packages from the last month", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AggreagationPackageDTO.class)))
    })
    public ResponseEntity<?> getMostUsedPackagesLastMonth(
        @Parameter(description = "Limit the number of results", example = "10", required = true) @PathVariable Integer limit
    ) {
        return restResponseMapper.map(ResultExecutor.execute(() -> (adminService.getMostUsedPackagesLastMonth(limit))), HttpStatus.OK);
    }

    @GetMapping("/topContributorLastMonth/{limit}")
    @Operation(
        summary = "Get the top contributors in the last month",
        description = "Returns the list of contributors who made the highest number of contributions during the last month, helping identify the most active maintainers in the ecosystem."
    )
    @ApiResponses({ 
        @ApiResponse(responseCode = "200", description = "Successfully retrieved the top contributors from the last month", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ContributorCountDTO.class)))
    })
    public ResponseEntity<?> getTopContributorLastMonth(
        @Parameter(description = "Limit the number of results", example = "10", required = true) @PathVariable Integer limit
    ) {
        return restResponseMapper.map(ResultExecutor.execute(() -> (adminService.getTopContributorsLastMonth(limit))), HttpStatus.OK);
    }

    @GetMapping("/packagesRiskBuckets")
    @Operation(
        summary = "Get packages aggregated by risk score buckets",
        description = "Returns packages grouped by their risk score into risk intervals (0-2, 2-4, 4-6, 6-8, 8-10), providing an overview of the risk distribution across the entire package ecosystem."
    )
    @ApiResponses({ 
        @ApiResponse(responseCode = "200", description = "Successfully retrieved packages aggregated by risk buckets", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RiskAggregationDTO.class)))
    })
    public ResponseEntity<?> getPackagesRiskBuckets() {
        return restResponseMapper.map(ResultExecutor.execute(() -> (adminService.getAggregateRiskBuckets())), HttpStatus.OK);
    }


    @GetMapping("/trendVulnerabilityLastYear")
    @Operation(
        summary = "Get vulnerability trend over the last year",
        description = "Returns the trend of reported vulnerabilities over the past year, providing insights into how the overall security landscape is evolving."
    )
    @ApiResponses({ 
        @ApiResponse(responseCode = "200", description = "Successfully retrieved the vulnerability trend over the last year", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ContributorCountDTO.class)))
    })
    public ResponseEntity<?> getTrendVulnerabilityLastYear() {
        return restResponseMapper.map(ResultExecutor.execute(() -> (adminService.getTrendVulnerabilityLastYear())), HttpStatus.OK);
    }

    @GetMapping("/mostDangerousVulnerabilities/{limit}")
    @Operation(
        summary = "Get the most critical vulnerabilities (Perfect Storm)",
        description = "Returns the list of vulnerabilities that are network-accessible (no authentication required), have low complexity to exploit, and have high or critical severity. These represent the most dangerous threats due to their ease of exploitation combined with severe impact."
    )
    @ApiResponses({ 
        @ApiResponse(responseCode = "200", description = "Successfully retrieved the most critical perfect storm vulnerabilities", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PerfectStormVulnerabilityDTO.class)))
    })
    public ResponseEntity<?> getPerfectStormVulnerabilities(
        @Parameter(description = "Limit the number of results", example = "10", required = true) @PathVariable Integer limit
    ) {
        return restResponseMapper.map(ResultExecutor.execute(() -> (adminService.getMostDangerousVulnerabilities(limit))), HttpStatus.OK);
    }
}