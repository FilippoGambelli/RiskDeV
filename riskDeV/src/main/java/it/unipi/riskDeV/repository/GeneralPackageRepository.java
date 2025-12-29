package it.unipi.riskDeV.repository;

import it.unipi.riskDeV.model.Package;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface GeneralPackageRepository extends MongoRepository<Package, String> {

}