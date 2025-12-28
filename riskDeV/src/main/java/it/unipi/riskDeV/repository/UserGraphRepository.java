package it.unipi.riskDeV.repository;

import it.unipi.riskDeV.model.neo4j.UserNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface UserGraphRepository extends Neo4jRepository<UserNode, String> {
    
}
