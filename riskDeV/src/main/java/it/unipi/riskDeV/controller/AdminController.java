package it.unipi.riskDeV.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.unipi.riskDeV.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;


@RestController
@RequestMapping("/api/admin")
@Tag(name = "Administrator Controller", description = "APIs for administrators")
@RequiredArgsConstructor
@Slf4j
public class AdminController {
    private final AdminService adminService;

    @PutMapping("addNewAdmin/{username}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Add New Administrator", description = "Add new administrator to the system")
    public String addNewAdmin(@PathVariable String username) {
        log.info("Trying to add new admin with username: {}", username);
        adminService.addNewAdmin(username);
        return "DONE";
    }

    @DeleteMapping("removeAdmin/{username}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Remove Administrator", description = "Remove administrator from the system")
    public String removeAdmin(@PathVariable String username) {
        log.info("Trying to remove admin with username: {}", username);
        adminService.removeAdmin(username);
        return "DONE";
    }
}
