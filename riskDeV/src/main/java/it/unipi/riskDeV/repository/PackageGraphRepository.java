package it.unipi.riskDeV.repository;

import it.unipi.riskDeV.model.neo4j.PackageNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

public interface PackageGraphRepository extends Neo4jRepository<PackageNode, String> {

    @Query("MATCH (p:Package {package_name: $packageName}) " +
           "MATCH (v:Version {package_name: $packageName, version: $version}) " +
           "MERGE (p)-[:HAS_VERSION]->(v)")
    void addVersionToPackage(@Param("packageName") String packageName, @Param("version") String version);

    Boolean existsByPackageName(String packageName);
}