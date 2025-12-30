package it.unipi.riskDeV.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.unipi.riskDeV.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;


@RestController
@RequestMapping("/api/projects")
@Tag(name = "Projects controller", description = "API for Projects operations")
@RequiredArgsConstructor
@Slf4j
public class ProjectController {
    
    private final ProjectService projectService;

    @GetMapping("/")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "List of all the projects in which the user is a collaborator",
            description = "Fetches a list of all the projects in which the user is a collaborator.")
    public List<String> getAllUserProjects() {
        log.info("Searching all the projects in which the user is a collaborator.");
        return projectService.getAllUserProjects();
    }
    
}
