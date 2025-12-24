package it.unipi.riskDeV.repository;

import it.unipi.riskDeV.model.GeneralPackage;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface GeneralPackageRepository extends MongoRepository<GeneralPackage, String> {

    // Optional is used in order to avoid null return if query fails
    Optional<GeneralPackage> findByPackage_name(String name);

}