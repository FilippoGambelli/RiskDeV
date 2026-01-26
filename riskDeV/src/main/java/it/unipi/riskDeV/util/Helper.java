package it.unipi.riskDeV.util;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import it.unipi.riskDeV.DTO.packageVersion.ConstraintsDTO;
import it.unipi.riskDeV.DTO.packageVersion.UpdateGeneralPackageDTO;
import it.unipi.riskDeV.model.documentDB.PackageVersion;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class Helper {
    private final VersionParser util;
    private final MongoTemplate mongoTemplate;

    public List<PackageVersion> addDependeciesGraph(String packageName, String version, List<ConstraintsDTO> dependenciesList) {
        List<PackageVersion> result = new ArrayList<>();
        
        for (ConstraintsDTO constraint : dependenciesList) {
            
            Criteria criteria = Criteria.where("package_name").is(constraint.getName());

            if (constraint.getVersionGte() != null) {
                criteria = criteria.and("version_array").gte(util.generateVersionArray(constraint.getVersionGte()));
            }
            if (constraint.getVersionLte() != null) {
                criteria = criteria.and("version_array").lte(util.generateVersionArray(constraint.getVersionLte()));
            }
            if (constraint.getVersionGt() != null) {
                criteria = criteria.and("version_array").gt(util.generateVersionArray(constraint.getVersionGt()));
            }
            if (constraint.getVersionLt() != null) {
                criteria = criteria.and("version_array").lt(util.generateVersionArray(constraint.getVersionLt()));
            }
            if (constraint.getVersionEq() != null) {
                criteria = criteria.and("version_array").is(util.generateVersionArray(constraint.getVersionEq()));
            }
            if (constraint.getVersionNeq() != null) {
                criteria = criteria.and("version_array").ne(util.generateVersionArray(constraint.getVersionNeq()));
            }

            Query query = new Query(criteria);

            result.addAll(mongoTemplate.find(query, PackageVersion.class));
        }

        return result;
    }

    public List<PackageVersion> updateDependeciesGraph(String packageName, String version) {
        Criteria dependencyCriteria = Criteria.where("requires_dist").elemMatch(
            new Criteria().andOperator(
                Criteria.where("name").is(packageName),
                new Criteria().orOperator(
                    Criteria.where("version_gte").lte(version),
                    Criteria.where("version_gte").is(null)
                ),
                new Criteria().orOperator(
                    Criteria.where("version_lte").gte(version),
                    Criteria.where("version_lte").is(null)
                ),
                new Criteria().orOperator(
                    Criteria.where("version_gt").lt(version),
                    Criteria.where("version_gt").is(null)
                ),
                new Criteria().orOperator(
                    Criteria.where("version_lt").gt(version),
                    Criteria.where("version_lt").is(null)
                ),
                new Criteria().orOperator(
                    Criteria.where("version_eq").is(version),
                    Criteria.where("version_eq").is(null)
                ),
                new Criteria().orOperator(
                    Criteria.where("version_neq").ne(version),
                    Criteria.where("version_neq").is(null)
                )
            )
        );

        Query query = new Query(dependencyCriteria);
        
        return mongoTemplate.find(query, PackageVersion.class);
    }

    public void updatePackageGeneralMetadata(String packageName, UpdateGeneralPackageDTO updateData) {
        Query query = new Query(Criteria.where("package_name").is(packageName));
        Update update = new Update();
        
        // For each Optional field: if a value is present, execute the lambda
        updateData.getAuthor().ifPresent(value -> update.set("author", value));
        updateData.getAuthorEmail().ifPresent(value -> update.set("author_email", value));
        updateData.getDescription().ifPresent(value -> update.set("description", value));
        updateData.getPackageURL().ifPresent(value -> update.set("package_url", value));
        updateData.getDocumentationURL().ifPresent(value -> update.set("documentation", value));

        // Only execute the update if there are fields to update
        if (!update.getUpdateObject().isEmpty()) {
            mongoTemplate.updateMulti(query, update, PackageVersion.class);
        }
    }

    public Double getMaxBaseScore(List<String> cveIds) {
        if (cveIds == null || cveIds.isEmpty()) {
            return 0.0;
        }

        // TODO: Before computing the aggregation, we should check if all CVEs are present in the database. 
        // If any are missing, we must request their information from the NVD API and add them to the database.
        
        Aggregation aggregation = Aggregation.newAggregation(
            Aggregation.match(Criteria.where("cve_id").in(cveIds)), 
            Aggregation.group().max("metrics.baseScore").as("maxBaseScore")
        );

        AggregationResults<MaxBaseScoreResult> results =
            mongoTemplate.aggregate(aggregation, "vulnerabilities", MaxBaseScoreResult.class);

        MaxBaseScoreResult result = results.getUniqueMappedResult();

        return result != null ? result.getMaxBaseScore() : 0.0;
    }
    
    private static class MaxBaseScoreResult {
        private Double maxBaseScore;

        public Double getMaxBaseScore() { return maxBaseScore; }
    }
}
