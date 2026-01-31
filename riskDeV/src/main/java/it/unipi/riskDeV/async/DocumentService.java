package it.unipi.riskDeV.async;

import org.springframework.stereotype.Service;

import it.unipi.riskDeV.repository.documentDB.PackageVersionRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DocumentService {
    private final PackageVersionRepository packageVersionRepository;

    public void updateRiskScore(String packageName, String version, Double riskScore) {
        packageVersionRepository.updateRiskScoreByPackageNameAndVersion(packageName, version, riskScore);
    }
}
