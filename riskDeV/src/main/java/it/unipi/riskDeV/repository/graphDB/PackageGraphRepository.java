package it.unipi.riskDeV.repository.graphDB;

import it.unipi.riskDeV.DTO.admin.CentralityResultDTO;
import it.unipi.riskDeV.model.graphDB.PackageNode;

import java.util.List;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

public interface PackageGraphRepository extends Neo4jRepository<PackageNode, String> {

    @Query(
        "MATCH (v:Version {package_name: $packageName, version: $version}) " +
        "MERGE (p:Package {package_name: $packageName}) " +
        "MERGE (p)-[:HAS_VERSION]->(v)"
        )
    void addVersionToPackage(@Param("packageName") String packageName, @Param("version") String version);

    // Degree Centrality – packages with the most direct dependents
    @Query("""
        MATCH (v:Version)<-[:DEPENDS_ON]-(d:Version)
        WITH v.package_name AS packageName, v.version AS packageVersion, count(DISTINCT d) AS score
        RETURN packageName, version, score
        ORDER BY score DESC
        LIMIT 10
    """)
    List<CentralityResultDTO> topByDegree();

    // PageRank – global impact of packages using pre-projected GDS graph
    @Query("""
        CALL gds.pageRank.stream('pkgGraph')
        YIELD nodeId, score
        WITH gds.util.asNode(nodeId) AS v, score
        RETURN v.package_name AS packageName, v.version AS packageVersion, score
        ORDER BY score DESC
        LIMIT 10
    """)
    List<CentralityResultDTO> topByPageRank();
}