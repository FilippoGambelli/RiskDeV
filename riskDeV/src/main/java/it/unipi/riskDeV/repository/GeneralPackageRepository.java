package it.unipi.riskDeV.repository;

import it.unipi.riskDeV.model.GeneralPackage;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface GeneralPackageRepository extends MongoRepository<GeneralPackage, String> {

    /**
     * Finds a GeneralPackage entity by its package name.
     * 
     * @param name The name of the package to search for.
     * @return An Optional containing the GeneralPackage if found or empty if no package with the given name exists.
     */
    Optional<GeneralPackage> findById(String name);

}