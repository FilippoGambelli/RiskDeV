package it.unipi.riskDeV.repository;

import it.unipi.riskDeV.model.PackageVersion;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface PackageVersionRepository extends MongoRepository<PackageVersion, String> {
    // Find the last version of a package
    Optional<PackageVersion> findTopByPackageNameOrderByVersionArrayDesc(String packageName);

    // Find a package using name and version
    Optional<PackageVersion> findByPackageNameAndVersion(String packageName, String version);

    // Check if exists the package using only it's name
    boolean existsByPackageName(String packageName);

    // Check if a specific version of a package exists
    boolean existsByPackageNameAndVersion(String packageName, String version);

    // Find safe versions of a package
    Optional<PackageVersion> findTopByPackageNameAndRiskScoreOrderByVersionArrayDesc(String packageName, int riskScore);

    void deleteByPackageNameAndVersion(String packageName, String version);
}