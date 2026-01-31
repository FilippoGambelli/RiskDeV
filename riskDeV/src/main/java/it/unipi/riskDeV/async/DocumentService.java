package it.unipi.riskDeV.async;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.unipi.riskDeV.repository.documentDB.PackageVersionRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DocumentService {
    private final PackageVersionRepository packageVersionRepository;

    @Transactional
    public void updateRiskScore(String packageName, String version, Double riskScore) {
        packageVersionRepository.updateRiskScoreByPackageNameAndVersion(packageName, version, riskScore);
    }
}
