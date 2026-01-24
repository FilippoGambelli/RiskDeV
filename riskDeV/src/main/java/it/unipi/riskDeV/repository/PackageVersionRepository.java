package it.unipi.riskDeV.repository;

import it.unipi.riskDeV.model.PackageVersion;
import it.unipi.riskDeV.model.Vulnerability;

import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface PackageVersionRepository extends MongoRepository<PackageVersion, String> {

    /*
    @Aggregation(pipeline = {
        "{ '$match': { '_id': ?0 } }",
        
        // 2. Esegui il Join con la collezione delle vulnerabilità
        // 'from': nome della collezione CVE nel database (es. 'vulnerabilities')
        // 'localField': il percorso dell'ID dentro l'oggetto pacchetto
        // 'foreignField': il campo _id nella collezione vulnerabilità
        "{ '$lookup': { 'from': 'vulnerabilities', 'localField': 'vulnerabilities.cve_id', 'foreignField': '_id', 'as': 'foundVulns' } }",
        
        // 3. 'Srotola' l'array trovato per avere una lista di oggetti separati
        "{ '$unwind': '$foundVulns' }",
        
        // 4. Sostituisce la radice del documento (che è il pacchetto) con la vulnerabilità trovata
        // Questo permette a Spring di mappare il risultato direttamente su List<Vulnerability>
        "{ '$replaceRoot': { 'newRoot': '$foundVulns' } }"
    })
    List<Vulnerability> findDirectVulnerabilities(String packageId);
    */

    // HO DOVUTO CAMBIARLA PERCHé ORA L'ID NON è PIù NOME + VERSIONE.
    // CONTROLLARE ANCHE FROM VULNERABILITIES O VULNERABILITY AL SINGOLARE, C'è UN MISMATCH CON VULNERABILITY MODEL
    @Aggregation(pipeline = {
        "{ '$match': { 'package_name': ?0, 'version': ?1 } }",
        "{ '$lookup': { 'from': 'vulnerabilities', 'localField': 'vulnerabilities.cve_id', 'foreignField': '_id', 'as': 'foundVulns' } }",
        "{ '$unwind': '$foundVulns' }",
        "{ '$replaceRoot': { 'newRoot': '$foundVulns' } }"
    })
    List<Vulnerability> findDirectVulnerabilities(String packageName, String version); 
    
    // Find a package using name and version
    Optional<PackageVersion> findByPackageNameAndVersion(String packageName, String version);

    // Check if exists the package using only it's name
    boolean existsByPackageName(String packageName);

    // Check if a specific version of a package exists
    boolean existsByPackageNameAndVersion(String packageName, String version);

    // Find the last version of a package
    Optional<PackageVersion> findTopByPackageNameOrderByVersionArrayDesc(String packageName);

    // Find all the versions of a package (used to propagate updating general package metadata to all the versions)
    List<PackageVersion> findByPackageName(String packageName);

    // Find safe versions of a package
    Optional<List<PackageVersion>> findTop5ByPackageNameAndRiskScoreOrderByVersionArrayDesc(String packageName, int riskScore);

    Optional<List<PackageVersion>> find(Query query);

    void updateMulti(Query query, Update update);

    void deleteByPackageNameAndVersion(String packageName, String version);
}