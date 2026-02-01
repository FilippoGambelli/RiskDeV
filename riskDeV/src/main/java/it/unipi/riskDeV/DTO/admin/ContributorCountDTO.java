package it.unipi.riskDeV.DTO.admin;

import io.swagger.v3.oas.annotations.media.Schema;

public class ContributorCountDTO {
    @Schema(description = "The username of the contributor", example = "john.doe")
    private String username;
    @Schema(description = "The number of project in which the user is a collaborator", example = "5")
    private Integer count;
}
