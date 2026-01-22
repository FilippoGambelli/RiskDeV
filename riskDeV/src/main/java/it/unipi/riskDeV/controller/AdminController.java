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
import it.unipi.riskDeV.controller.util.RestResponseMapper;
import it.unipi.riskDeV.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/api/admin")
@Tag(
    name = "Administrator Controller", 
    description = "APIs for managing administrators"
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

    @PutMapping("addNewAdmin/{username}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Add New Administrator", 
        description = "Promotes an existing user to administrator role"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Administrator added successfully"
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
        log.info("Trying to add new admin with username: {}", username);
        return restResponseMapper.map(adminService.addNewAdmin(username), HttpStatus.OK);
    }

    @DeleteMapping("removeAdmin/{username}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Remove Administrator", 
        description = "Revokes administrator privileges from an existing administrator"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Administrator removed successfully"
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
        log.info("Trying to remove admin with username: {}", username);
        return restResponseMapper.map(adminService.removeAdmin(username), HttpStatus.OK);
    }
}