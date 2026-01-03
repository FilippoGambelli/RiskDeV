package it.unipi.riskDeV.repository;

import it.unipi.riskDeV.model.PackageVersion;
import it.unipi.riskDeV.model.Vulnerability;

import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface PackageVersionRepository extends MongoRepository<PackageVersion, String> {

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

    @Query("{ 'package_name': ?0, 'vulnerabilities': [] }")
    List<PackageVersion> findSafeVersions(String packageName);

}