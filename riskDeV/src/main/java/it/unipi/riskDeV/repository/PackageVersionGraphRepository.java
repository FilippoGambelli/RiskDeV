package it.unipi.riskDeV.repository;

import it.unipi.riskDeV.model.neo4j.PackageVersionNode;
import it.unipi.riskDeV.DTO.DependencyDTO;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;

public interface PackageVersionGraphRepository extends Neo4jRepository<PackageVersionNode, String> {

    // Find packages that depend from the target package
    @Query("MATCH (p:Package {id: $packageName})-[:HAS_VERSION]->(v:Version) " +
           "MATCH (dependent:Version)-[:DEPENDS_ON]->(v) " +
           "RETURN DISTINCT dependent")
    List<PackageVersionNode> findReverseDependencies(@Param("packageName") String packageName);

    // Find all the dependeces of a target package
    @Query("MATCH (v:Version {id: $versionId})-[r:DEPENDS_ON]->(target:Version) " +
           "RETURN target.id AS targetPackage, target.version AS targetVersion, r.constraint AS constraint")
    List<DependencyDTO> findDirectDependencies(@Param("versionId") String versionId);

    @Query("MATCH (v:Version {id: $versionId}) " +
           "UNWIND $cveIds AS cve " +
           "MERGE (vuln:Vulnerability {id: cve}) " +
           "MERGE (v)-[:AFFECTED_BY]->(vuln)")
    void attachVulnerabilities(@Param("versionId") String versionId, @Param("cveIds") List<String> cveIds);

    @Query("MATCH (v:Version {id: $versionId}) " +
           "UNWIND $dependencyNames AS depName " +
           "MERGE (target:Package {id: depName}) " +
           "MERGE (v)-[:DEPENDS_ON]->(target)")
    void attachDependencies(@Param("versionId") String versionId, @Param("dependencyNames") List<String> dependencyNames);

    @Query("""
        MATCH (source:Version {id: $sourceId})
        UNWIND $dependenciesList AS dep
        
        MERGE (target:Version {id: dep.targetId})
        ON CREATE SET 
            target.version = dep.version,
            target.isStub = true  
        MERGE (source)-[r:DEPENDS_ON]->(target)
        SET r.constraint = dep.operator
        
        MERGE (p:Package {id: dep.pkgName})
        ON CREATE SET p.isStub = true
       
        MERGE (p)-[:HAS_VERSION]->(target)
    """)
    void attachDependenciesWithStubs(@Param("sourceId") String sourceId, @Param("dependenciesList") List<Map<String, String>> dependenciesList);

    // Delete old dependencies (used during manual update of a version)
    @Query("MATCH (v:Version {id: $versionId})-[r:DEPENDS_ON]->() DELETE r")
    void deleteDependencies(@Param("versionId") String versionId);

    // Delete vulnerabilities (used during manual update of a version)
    @Query("MATCH (v:Version {id: $versionId})-[r:AFFECTED_BY]->() DELETE r")
    void deleteVulnerabilities(@Param("versionId") String versionId);
}