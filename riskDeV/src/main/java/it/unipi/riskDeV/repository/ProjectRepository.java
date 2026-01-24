package it.unipi.riskDeV.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import it.unipi.riskDeV.model.Project;

public interface ProjectRepository extends MongoRepository<Project, String> {
    Optional<Project> findByName(String name);
}
