package it.unipi.riskDeV.repository;

import it.unipi.riskDeV.model.neo4j.PackageVersionNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PackageVersionGraphRepository extends Neo4jRepository<PackageVersionNode, String> {

    // Find packages that depend from the target package
    @Query("MATCH (p:Package {id: $packageName})-[:HAS_VERSION]->(v:Version) " +
           "MATCH (dependent:Version)-[:DEPENDS_ON]->(v) " +
           "RETURN DISTINCT dependent")
    List<PackageVersionNode> findReverseDependencies(@Param("packageName") String packageName);

    // Find all the dependeces of a target package
    @Query("MATCH (v:Version {id: $versionId})-[:DEPENDS_ON]->(target:Version) " +
           "RETURN DISTINCT target")
    List<PackageVersionNode> findDirectDependencies(@Param("versionId") String versionId);
}