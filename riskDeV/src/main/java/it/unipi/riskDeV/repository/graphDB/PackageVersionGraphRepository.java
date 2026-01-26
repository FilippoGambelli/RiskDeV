package it.unipi.riskDeV.repository.graphDB;

import it.unipi.riskDeV.DTO.vulnerability.VulnerabilityReportDTO;
import it.unipi.riskDeV.model.graphDB.PackageVersionNode;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PackageVersionGraphRepository extends Neo4jRepository<PackageVersionNode, String> {

    Boolean existsByPackageNameAndVersion(String packageName, String version);

    @Query("""
            MATCH (target:Version {package_name: $packageName, version: $version})
            MATCH (dependent:Version)-[:DEPENDS_ON]->(target)
            RETURN dependent
        """)
    List<PackageVersionNode> findReverseDependencies(@Param("packageName") String packageName, @Param("version") String version);

    @Query("""
            MATCH (v:Version {package_name: $packageName, version: $version})
            MERGE (vuln:Vulnerability {cve_id: $cveId})
            MERGE (v)-[:AFFECTED_BY]->(vuln)
            """)
    void attachVulnerability(@Param("packageName") String packageName, @Param("version") String version, @Param("cveId") String cveIds);

    @Query("""
        MATCH (source:Version {package_name: $sourcePackageName, version: $sourceVersion})
        MERGE (target:Version {package_name: $targetPackageName, version: $targetVersion})
        MERGE (source)-[r:DEPENDS_ON]->(target)
        """)
    void attachDependency(@Param("sourcePackageName") String sourcePackageName, @Param("sourceVersion") String sourceVersion,
                          @Param("targetPackageName") String targetPackageName, @Param("targetVersion") String targetVersion);

    @Query("MATCH (v:Version {package_name: $packageName, version: $version})-[r:DEPENDS_ON]->() DELETE r")
    void deleteDependencies(@Param("packageName") String packageName, @Param("version") String version);

    @Query("MATCH (v:Version {package_name: $packageName, version: $version})-[r:AFFECTED_BY]->() DELETE r")
    void deleteVulnerabilities(@Param("packageName") String packageName, @Param("version") String version);

    @Query("""
        MATCH (v:Version {package_name: $packageName})
        SET v.documentation = $newDocumentationURL
        """)
    void updateDocumentation(@Param("packageName") String packageName, @Param("newDocumentationURL") String newDocumentationURL);

    void deleteByPackageNameAndVersion(String packageName, String version);

    @Query("MATCH (start:Version {package_name: $package_name, version: $version}) " +
        "MATCH (start)-[:DEPENDS_ON*]->(dep:Version) " +
        "WITH DISTINCT dep " +
        "ORDER BY dep.version_array DESC " +
        "WITH dep.package_name AS pkgName, head(collect(dep)) AS maxVersion " +
        "MATCH (maxVersion)-[:AFFECTED_BY]->(vuln:Vulnerability) " +
        "RETURN vuln.cve_id AS vulnerabilityId, " +
                "pkgName AS affectedPackage, " +
                "maxVersion.version AS affectedVersion, " +
                "vuln.description AS description, " +
                "vuln.exploitabilityScore AS exploitabilityScore, " +
                "vuln.baseScore AS baseScore")
    List<VulnerabilityReportDTO> findRecursiveVulnerabilities(@Param("package_name") String packageName, @Param("version") String packageVersion);
}