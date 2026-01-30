package it.unipi.riskDeV.service;

import java.util.List;

import org.springframework.stereotype.Service;

import it.unipi.riskDeV.repository.documentDB.UserRepository;
import it.unipi.riskDeV.repository.graphDB.PackageGraphRepository;
import it.unipi.riskDeV.results.DomainError;
import it.unipi.riskDeV.results.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import it.unipi.riskDeV.DAO.PackageDAO;
import it.unipi.riskDeV.DAO.ProjectDAO;
import it.unipi.riskDeV.DAO.VulnerabilityDAO;
import it.unipi.riskDeV.DTO.MessageResponseDTO;
import it.unipi.riskDeV.DTO.admin.AggreagationPackageDTO;
import it.unipi.riskDeV.DTO.admin.CentralityResultDTO;
import it.unipi.riskDeV.DTO.admin.ContributorCountDTO;
import it.unipi.riskDeV.DTO.admin.PageRankResultDTO;
import it.unipi.riskDeV.DTO.admin.PerfectStormVulnerabilityDTO;
import it.unipi.riskDeV.DTO.admin.RiskAggregationDTO;
import it.unipi.riskDeV.DTO.admin.VulnerabilityTrendDTO;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {
    private final UserRepository userRepository;
    private final PackageGraphRepository PackageGraphRepository;
    private final ProjectDAO projectDAO;
    private final PackageDAO packageDAO;
    private final VulnerabilityDAO vulnerabilityDAO;

    public Result<MessageResponseDTO> addNewAdmin(String username) {
        
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
            return new Result.Failure<>(new DomainError.SystemError(e));
        }

        return new Result.Success<>(new MessageResponseDTO("Administrator added successfully"));
    }

    public Result<MessageResponseDTO> removeAdmin(String username) {
        
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
            return new Result.Failure<>(new DomainError.SystemError(e));
        }

        return new Result.Success<>(new MessageResponseDTO("Administrator removed successfully"));
    }

    public Result<List<CentralityResultDTO>> getTopByDegree() {
        var results = PackageGraphRepository.topByDegree();
        return new Result.Success<>(results);
    }

    public Result<List<PageRankResultDTO>> getTopByPageRank(Integer limit) {
        var results = PackageGraphRepository.topByPageRank(limit);
        return new Result.Success<>(results);
    }

    public Result<List<AggreagationPackageDTO>> getMostUsedPackages(int limit) {
        return new Result.Success<>(projectDAO.mostUsedPackages(limit));
    }

    public Result<List<AggreagationPackageDTO>> getMostUsedPackagesLastMonth(int limit) {
        return new Result.Success<>(projectDAO.mostUsedPackagesLastMonth(limit));    
    }

    public Result<List<ContributorCountDTO>> getTopContributorsLastMonth(int limit) {
        List<ContributorCountDTO> collaborators = projectDAO.getTopCollaboratorsLastMonth();
        return new Result.Success<>(collaborators);
    }

    public Result<RiskAggregationDTO> getAggregateRiskBuckets() {
        return new Result.Success<>(packageDAO.aggregateRiskBuckets());    
    }

    public Result<List<VulnerabilityTrendDTO>> getTrendVulnerabilityLastYear() {
        return new Result.Success<>(vulnerabilityDAO.getVulnerabilityTrendLastYear());
    }

    public Result<List<PerfectStormVulnerabilityDTO>> getMostDangerousVulnerabilities(int limit) {
        return new Result.Success<>(vulnerabilityDAO.getMostDangerousVulnerabilities(limit));
        
    }
}