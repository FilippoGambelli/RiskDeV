package it.unipi.riskDeV.repository;

import it.unipi.riskDeV.model.PackageVersion;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface PackageVersionRepository extends MongoRepository<PackageVersion, String> {

    // Optional is used in order to avoid null return if query fails
    Optional<PackageVersion> findByPackageNameAndVersion(String name, String version);

}