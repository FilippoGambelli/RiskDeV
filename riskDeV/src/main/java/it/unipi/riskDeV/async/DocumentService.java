package it.unipi.riskDeV.async;

import org.springframework.stereotype.Service;

import it.unipi.riskDeV.repository.documentDB.PackageVersionRepository;
import it.unipi.riskDeV.results.DomainError;
import it.unipi.riskDeV.results.Result;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DocumentService {
    private final PackageVersionRepository packageVersionRepository;

    public Result<Void> updateRiskScore(String packageName, String version, Double risk_score) {
        try {
            packageVersionRepository.updateRiskScoreByPackageNameAndVersion(packageName, version, risk_score);
            return new Result.Success<>(null);
        } catch (Exception e) {
            return new Result.Failure<>(new DomainError.SystemError());
        }
    }
}
