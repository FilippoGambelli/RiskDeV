package it.unipi.riskDeV.repository;

import it.unipi.riskDeV.model.neo4j.PackageVersionNode;
import it.unipi.riskDeV.DTO.DependencyDTO;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;

// Extend <..., Long> because the new script uses @Id Long id (internal id)
public interface PackageVersionGraphRepository extends Neo4jRepository<PackageVersionNode, Long> {

    // Find packages that depend on THIS package version
    @Query("MATCH (target:Version {package_name: $pkgName, version: $verNum}) " +
           "MATCH (dependent:Version)-[:DEPENDS_ON]->(target) " +
           "RETURN dependent")
    List<PackageVersionNode> findReverseDependencies(@Param("pkgName") String pkgName, 
                                                     @Param("verNum") String verNum);

    // Find direct dependencies of a specific package
    @Query("MATCH (source:Version {package_name: $pkgName, version: $verNum})-[r:DEPENDS_ON]->(target:Version) " +
           "RETURN target.package_name AS targetPackage, target.version AS targetVersion, r.constraint AS constraint")
    List<DependencyDTO> findDirectDependencies(@Param("pkgName") String pkgName, 
                                               @Param("verNum") String verNum);

    // Attach Vulnerabilities
    @Query("MATCH (v:Version {package_name: $pkgName, version: $verNum}) " +
           "UNWIND $cveIds AS cve " +
           "MERGE (vuln:Vulnerability {cve_id: cve}) " +
           "MERGE (v)-[:AFFECTED_BY]->(vuln)")
    void attachVulnerabilities(@Param("pkgName") String pkgName, 
                               @Param("verNum") String verNum, 
                               @Param("cveIds") List<String> cveIds);

    // Attach Dependencies, match only if target version exists
    @Query("""
        MATCH (source:Version {package_name: $srcPkg, version: $srcVer})
        UNWIND $dependenciesList AS dep
        
        // Searching the exact version of the dependency
        MATCH (target:Version {package_name: dep.pkgName, version: dep.version})
            
        // If the target version node exists, they are linked
        MERGE (source)-[r:DEPENDS_ON]->(target)
        SET r.constraint = dep.constraint
    """)
    void attachDependencies(@Param("srcPkg") String srcPkg, 
                            @Param("srcVer") String srcVer, 
                            @Param("dependenciesList") List<Map<String, String>> dependenciesList);

    // Original attachDependency
    /*
    @Query("MATCH (v:Version {id: $versionId}) " +
           "UNWIND $dependencyNames AS depName " +
           "MERGE (target:Package {id: depName}) " +
           "MERGE (v)-[:DEPENDS_ON]->(target)")
    void attachDependencies(@Param("versionId") String versionId, @Param("dependencyNames") List<String> dependencyNames);
    */

    // Attach Dependencies with stubs 
    // TODO: maybe it can be deleted we have to think about automatic download, in the doubt there are both versions here
    @Query("""
        MATCH (source:Version {package_name: $srcPkg, version: $srcVer})
        UNWIND $dependenciesList AS dep
        
        // Target version
        MERGE (target:Version {package_name: dep.pkgName, version: dep.version})
        ON CREATE SET 
            target.isStub = true,
            target.version_array = [0,0,0,0,0,0] // TODO: initialize version_array if used
            
        // Relationship
        MERGE (source)-[r:DEPENDS_ON]->(target)
        SET r.constraint = dep.operator
        
        // Package node target
        MERGE (p:Package {package_name: dep.pkgName})
        ON CREATE SET p.isStub = true
        MERGE (p)-[:HAS_VERSION]->(target)
    """)
    void attachDependenciesWithStubs(@Param("srcPkg") String srcPkg, 
                                     @Param("srcVer") String srcVer, 
                                     @Param("dependenciesList") List<Map<String, String>> dependenciesList);

    // Original attachDependenciesWithStubs
    /*
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
    */

    // Delete dependencies of a version node
    @Query("MATCH (v:Version {package_name: $pkgName, version: $verNum})-[r:DEPENDS_ON]->() DELETE r")
    void deleteDependencies(@Param("pkgName") String pkgName, @Param("verNum") String verNum);

    // Delete vulnerabilities of a version node
    @Query("MATCH (v:Version {package_name: $pkgName, version: $verNum})-[r:AFFECTED_BY]->() DELETE r")
    void deleteVulnerabilities(@Param("pkgName") String pkgName, @Param("verNum") String verNum);
}