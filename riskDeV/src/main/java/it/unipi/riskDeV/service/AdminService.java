package it.unipi.riskDeV.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import it.unipi.riskDeV.repository.PackageCentralityRepository;
import it.unipi.riskDeV.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import it.unipi.riskDeV.DAO.PackageDAO;
import it.unipi.riskDeV.DAO.ProjectDAO;
import it.unipi.riskDeV.DAO.VulnerabilityDAO;
import it.unipi.riskDeV.DTO.admin.AggreagationPackageDTO;
import it.unipi.riskDeV.DTO.admin.CentralityResultDTO;
import it.unipi.riskDeV.DTO.admin.ContributorCountDTO;
import it.unipi.riskDeV.DTO.admin.PackageRiskTrendDTO;
import it.unipi.riskDeV.DTO.admin.PerfectStormVulnerabilityDTO;
import it.unipi.riskDeV.DTO.admin.SeverityDistributionDTO;
import it.unipi.riskDeV.DTO.admin.VulnerabilityTrendDTO;
import it.unipi.riskDeV.common.DomainError;
import it.unipi.riskDeV.common.Result;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {
    private final UserRepository userRepository;
    private final PackageCentralityRepository packageCentralityRepository;
    private final ProjectDAO projectDAO;
    private final PackageDAO packageDAO;
    private final VulnerabilityDAO vulnerabilityDAO;

    public Result<List<CentralityResultDTO>> getTopByDegree() {
        var results = packageCentralityRepository.topByDegree();
        return new Result.Success<>(results);
    }

    public Result<List<CentralityResultDTO>> getTopByPageRank() {
        var results = packageCentralityRepository.topByPageRank();
        return new Result.Success<>(results);
    }


    public Result<String> addNewAdmin(String username) {
        
        var optionalUser = userRepository.findByUsername(username);

        if (optionalUser.isEmpty()) {
            return new Result.Failure<>(new DomainError.NotFound("User " + username + " does not exist"));
        }

        var user = optionalUser.get();

        if (user.getRole().equals("ROLE_ADMIN")) {
            return new Result.Failure<>(new DomainError.AlreadyExists("User " + username + " is already an administrator"));
        }

        user.setRole("ROLE_ADMIN");

        try {
            userRepository.save(user);
            log.info("Administrator {} was successfully added", username);
        } catch (Exception e) {
            log.warn("Failed to save administrator with username {}", username);
            return new Result.Failure<>(new DomainError.SystemError("Failed to save administrator", e));
        }

        return new Result.Success<>("Administrator was successfully added");
    }

    public Result<String> removeAdmin(String username) {
        
        var optionalUser = userRepository.findByUsername(username);

        if (optionalUser.isEmpty()) {
            return new Result.Failure<>(new DomainError.NotFound("User " + username + " does not exist"));
        }

        var user = optionalUser.get();
        
        if (user.getRole().equals("ROLE_USER")) {
            return new Result.Failure<>(new DomainError.AlreadyExists("User " + username + " is already a standard user"));
        }

        user.setRole("ROLE_USER");

        try {
            userRepository.save(user);
            log.info("Administrator {} was successfully removed", username);
        } catch (Exception e) {
            log.warn("Failed to remove administrator with username {}", username);
            return new Result.Failure<>(new DomainError.SystemError("Failed to remove administrator", e));
        }

        return new Result.Success<>("Administrator successfully removed");
    }

    public Result<List<AggreagationPackageDTO>> getMostUsedPackages(int limit) {
        return new Result.Success<>(projectDAO.mostUsedPackages(limit));
    }

    public Result<List<AggreagationPackageDTO>> getMostUsedPackagesLastMonth(int limit) {
        return new Result.Success<>(projectDAO.mostUsedPackagesLastMonth(limit));    
    }


    public Result<List<ContributorCountDTO>> getTopContributorsLastMonth(int limit) {
        List<ContributorCountDTO> admins = projectDAO.getTopAdminsLastMonth();
        List<ContributorCountDTO> collaborators = projectDAO.getTopCollaboratorsLastMonth();

        Map<String, Integer> contributorMap = new HashMap<>();

        if (admins != null) {
            admins.forEach(c -> contributorMap.merge(
                                            c.username(),
                                            c.count(),
                                            (oldVal, newVal) -> oldVal + newVal
                                ));
        }

        if (collaborators != null) {
            collaborators.forEach(c -> contributorMap.merge(
                                            c.username(),
                                            c.count(),
                                            (oldVal, newVal) -> oldVal + newVal
                                ));
        }

        List<ContributorCountDTO> result = contributorMap.entrySet().stream()
                                        .map(e -> new ContributorCountDTO(e.getKey(), e.getValue()))
                                        .sorted((a, b) -> Integer.compare(b.count(), a.count()))
                                        .limit(limit)
                                        .toList();

        return new Result.Success<>(result);
    }

    public Result<List<PackageRiskTrendDTO>> getPackagesWithNegativeRiskTrend(int limit) {
        return new Result.Success<>(packageDAO.getPackagesWithNegativeRiskTrend(limit));    
    }

    public Result<List<VulnerabilityTrendDTO>> getTrendVulnerabilityLastYear() {
        return new Result.Success<>(vulnerabilityDAO.getVulnerabilityTrendLastYear());
    }

    public Result<List<SeverityDistributionDTO>> getSeverityDistributionLastYear() {
        return new Result.Success<>(vulnerabilityDAO.getSeverityDistributionLastYear());
    }

    public Result<List<PerfectStormVulnerabilityDTO>> getPerfectStormVulnerabilities(int limit) {
        return new Result.Success<>(vulnerabilityDAO.getPerfectStormVulnerabilities(limit));
    }
}