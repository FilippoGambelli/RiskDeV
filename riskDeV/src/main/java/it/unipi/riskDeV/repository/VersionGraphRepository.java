package it.unipi.riskDeV.repository;

import it.unipi.riskDeV.model.neo4j.PackageVersionNode;
import it.unipi.riskDeV.DTO.VulnerabilityReportDTO;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface VersionGraphRepository extends Neo4jRepository<PackageVersionNode, String> {

    @Query("MATCH (start:Version {package_name: $package_name, version: $version}) " +
           "MATCH (start)-[:DEPENDS_ON*]->(dep:Version) " +
           "WITH DISTINCT dep " +
           // Ordina usando l'array di interi per precisione (es. 1.10 > 1.2)
           "ORDER BY dep.version_array DESC " +
           // Raggruppa per pacchetto e prende solo la versione più alta trovata nel grafo delle dipendenze
           "WITH dep.package_name AS pkgName, head(collect(dep)) AS maxVersion " +
           "MATCH (maxVersion)-[:AFFECTED_BY]->(vuln:Vulnerability) " +
           "RETURN vuln.cve_id AS vulnerabilityId, " +
                  "pkgName AS affectedPackage, " +
                  "maxVersion.version AS affectedVersion, " +
                  "vuln.description AS description, " +
                  "vuln.exploitabilityScore AS exploitabilityScore, " +
                  "vuln.baseScore AS baseScore")
    List<VulnerabilityReportDTO> findRecursiveVulnerabilities(
            @Param("package_name") String packageName, 
            @Param("version") String packageVersion
    );
}