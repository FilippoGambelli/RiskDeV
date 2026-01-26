package it.unipi.riskDeV.repository.documentDB;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import it.unipi.riskDeV.model.documentDB.Project;

public interface ProjectRepository extends MongoRepository<Project, String> {
    Optional<Project> findByName(String name);
}
