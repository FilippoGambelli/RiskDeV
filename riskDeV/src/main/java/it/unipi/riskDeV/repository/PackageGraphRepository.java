package it.unipi.riskDeV.repository;

import it.unipi.riskDeV.model.neo4j.PackageNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;


public interface PackageGraphRepository extends Neo4jRepository<PackageNode, String> {
    
}