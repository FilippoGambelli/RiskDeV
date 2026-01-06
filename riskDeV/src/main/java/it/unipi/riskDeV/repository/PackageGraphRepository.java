package it.unipi.riskDeV.repository;

import it.unipi.riskDeV.model.neo4j.PackageNode;

import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;


public interface PackageGraphRepository extends Neo4jRepository<PackageNode, String> {

    @Query("MATCH (p:Package {id: $packageName}) " +
           "MATCH (v:Version {id: $versionId}) " +
           "MERGE (p)-[:HAS_VERSION]->(v)")
    void addVersionToPackage(@Param("packageName") String packageName, @Param("versionId") String versionId);

}