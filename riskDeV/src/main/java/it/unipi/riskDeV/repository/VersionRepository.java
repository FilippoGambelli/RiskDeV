package it.unipi.riskDeV.repository;

import it.unipi.riskDeV.model.neo4j.VersionNode;
import it.unipi.riskDeV.DTO.VulnerabilityReportDTO;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface VersionRepository extends Neo4jRepository<VersionNode, String> {

    @Query("MATCH (start:Version {id: $id}) " +
           // 1. RICORSIONE PROFONDA
           // Troviamo tutte le dipendenze, dirette e indirette
           "MATCH (start)-[:DEPENDS_ON*]->(v:Version) " +
           
           // 2. DEDUPLICAZIONE
           // Fondamentale per evitare calcoli ripetuti su nodi raggiunti da più parti
           "WITH DISTINCT v " +

           // 3. RAGGRUPPAMENTO DIRETTO (Senza saltare al Package)
           // Assumiamo che 'v.packageName' esista. Raggruppiamo per questo nome.
           // Manteniamo 'v' per poterlo ordinare nel passaggio successivo.
           "WITH v.name AS pkgName, v " +

           // 4. ORDINAMENTO SEMANTICO
           // Ordiniamo le versioni DI QUEL GRUPPO (pacchetto) dalla più recente alla più vecchia
           "ORDER BY v.major DESC, v.minor DESC, v.patch DESC " +

           // 5. SELEZIONE VINCITORE
           // Prendiamo la testa della lista (la versione più alta) per quel pkgName
           "WITH pkgName, head(collect(v)) AS maxVersion " +

           // 6. CHECK VULNERABILITÀ
           // Controlliamo se la versione vincitrice è affetta da vulnerabilità
           "MATCH (maxVersion)-[:AFFECTED_BY]->(vuln:Vulnerability) " +

           // 7. RETURN DTO
           "RETURN vuln.id AS vulnerabilityId, " +
                  "pkgName AS affectedPackage, " +
                  "maxVersion.version AS affectedVersion")
    List<VulnerabilityReportDTO> findRecursiveVulnerabilities(@Param("id") String id);
}