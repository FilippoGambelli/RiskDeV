package it.unipi.riskDeV.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import it.unipi.riskDeV.model.Project;

public interface ProjectRepository extends MongoRepository<Project, String> {
    
}
