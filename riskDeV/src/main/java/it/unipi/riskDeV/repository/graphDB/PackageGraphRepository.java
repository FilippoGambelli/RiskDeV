package it.unipi.riskDeV.repository.graphDB;

import it.unipi.riskDeV.DTO.admin.CentralityResultDTO;
import it.unipi.riskDeV.DTO.admin.PageRankResultDTO;
import it.unipi.riskDeV.model.graphDB.PackageNode;

import java.util.List;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

public interface PackageGraphRepository extends Neo4jRepository<PackageNode, String> {

    @Query("""
        MATCH (v:Version {package_name: $packageName, version: $version})
        MERGE (p:Package {package_name: $packageName})
        MERGE (p)-[:HAS_VERSION]->(v)
    """)
    void addVersionToPackage(@Param("packageName") String packageName, @Param("version") String version);

    @Query("""
        MATCH (v:Version)<-[:DEPENDS_ON]-(d:Version)
        WITH v.package_name AS package_name, count(DISTINCT d) AS score
        RETURN package_name, score
        ORDER BY score DESC
        LIMIT 10
    """)
    List<CentralityResultDTO> topByDegree();

    @Query("""
        CALL gds.pageRank.stream('pkgGraph')
        YIELD nodeId, score
        WITH gds.util.asNode(nodeId) AS v, score
        RETURN v.package_name AS package_name, v.version AS version, score
        ORDER BY score DESC
        LIMIT $limit
    """)
    List<PageRankResultDTO> topByPageRank(@Param("limit") Integer limit);
}