package it.unipi.riskDeV.repository;

import it.unipi.riskDeV.model.neo4j.PackageVersionNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PackageVersionGraphRepository extends Neo4jRepository<PackageVersionNode, String> {

    @Query("MATCH (p:Package {id: $packageName})-[:HAS_VERSION]->(v:Version) " +
           "MATCH (dependent:Version)-[:DEPENDS_ON]->(v) " +
           "RETURN DISTINCT dependent")
    List<PackageVersionNode> findReverseDependencies(@Param("packageName") String packageName);
}