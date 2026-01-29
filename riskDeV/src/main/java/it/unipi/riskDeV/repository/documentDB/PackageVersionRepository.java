package it.unipi.riskDeV.repository.documentDB;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;

import it.unipi.riskDeV.model.documentDB.PackageVersion;

import java.util.List;
import java.util.Optional;

public interface PackageVersionRepository extends MongoRepository<PackageVersion, String> {
    Optional<PackageVersion> findTopByPackageNameOrderByVersionArrayDesc(String packageName);

    Optional<PackageVersion> findByPackageNameAndVersion(String packageName, String version);

    boolean existsByPackageName(String packageName);

    boolean existsByPackageNameAndVersion(String packageName, String version);

    Optional<PackageVersion> findTopByPackageNameAndRiskScoreOrderByVersionArrayDesc(String packageName, int riskScore);

    List<PackageVersion> findByPackageNameAndRiskScoreOrderByVersionArrayDesc(String packageName, int riskScore);
    
    void deleteByPackageNameAndVersion(String packageName, String version);

    @Query("{ 'packageName': ?0, 'version': ?1 }")
    @Update("{ '$set': { 'riskScore': ?2 } }")
    void updateRiskScoreByPackageNameAndVersion(String packageName, String version, Double riskScore);
}