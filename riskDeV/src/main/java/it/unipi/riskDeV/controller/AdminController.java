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

import it.unipi.riskDeV.DTO.CentralityResultDTO;
import it.unipi.riskDeV.DTO.ErrorResponseDTO;
import it.unipi.riskDeV.controller.util.RestResponseMapper;
import it.unipi.riskDeV.service.AdminService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/api/admin")
@Tag(
    name = "Administrator Controller",
    description = "APIs for managing administrator actions and retrieving package centrality information"
)
@RequiredArgsConstructor
@Slf4j
@ApiResponses(value = {
    @ApiResponse(
        responseCode = "500",
        description = "Internal server error",
        content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
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
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved the top packages by degree centrality",
            content = @Content(schema = @Schema(implementation = CentralityResultDTO.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "No packages found",
            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
        )
    })
    public ResponseEntity<?> getTopByDegree() {
        log.info("Retrieving top packages by degree centrality");
        return restResponseMapper.map(adminService.getTopByDegree(), HttpStatus.OK);
    }

    @GetMapping("/packagesWithPageRank")
    @Operation(
        summary = "Get the top packages by PageRank",
        description = "Returns the list of packages ranked by PageRank score, representing their global influence across the dependency network. Packages with higher scores impact a larger portion of the ecosystem."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved the top packages by PageRank",
            content = @Content(schema = @Schema(implementation = CentralityResultDTO.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "No packages found",
            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
        )
    })
    public ResponseEntity<?> getTopByPageRank() {
        log.info("Retrieving top packages by PageRank centrality");
        return restResponseMapper.map(adminService.getTopByPageRank(), HttpStatus.OK);
    }


    @PutMapping("addNewAdmin/{username}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Add a new administrator",
        description = "Promotes an existing user to have administrator privileges."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "User successfully promoted to administrator"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "User not found",
            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
        )
    })
    public ResponseEntity<?> addNewAdmin(
        @Parameter(
            description = "The username of the user to be promoted to administrator",
            example = "david.russo",
            required = true,
            schema = @Schema(type = "string")
        )
        @PathVariable String username
    ) {
        log.info("Adding new administrator: {}", username);
        return restResponseMapper.map(adminService.addNewAdmin(username), HttpStatus.OK);
    }

    @DeleteMapping("removeAdmin/{username}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Remove an administrator",
        description = "Revokes administrator privileges from an existing administrator."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Administrator successfully removed"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Administrator not found",
            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
        )
    })
    public ResponseEntity<?> removeAdmin(
        @Parameter(
            description = "The username of the administrator to be removed",
            example = "david.russo",
            required = true,
            schema = @Schema(type = "string")
        )
        @PathVariable String username
    ) {
        log.info("Removing administrator: {}", username);
        return restResponseMapper.map(adminService.removeAdmin(username), HttpStatus.OK);
    }
}