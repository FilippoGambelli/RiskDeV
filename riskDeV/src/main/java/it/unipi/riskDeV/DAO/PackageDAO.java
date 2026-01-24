package it.unipi.riskDeV.DAO;

import java.util.List;

import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Sort;
import it.unipi.riskDeV.DTO.PackageRiskTrendDTO;

@Repository
public class PackageDAO {
    
    private MongoTemplate mongoTemplate;

    public List<PackageRiskTrendDTO> getPackagesWithNegativeRiskTrend(int limit) {
        Aggregation aggregation = Aggregation.newAggregation(
            Aggregation.sort(Sort.by("package_name", "version_array")),

            context -> new Document("$setWindowFields",
                new Document("partitionBy", "$package_name")
                    .append("sortBy", new Document("version_array", 1))
                    .append("output", new Document()
                        .append("prevRisk",
                            new Document("$shift",
                                new Document("output", "$risk_score").append("by", -1)))
                        .append("prevVulnCount",
                            new Document("$shift",
                                new Document("output",
                                    new Document("$size", "$vulnerabilities"))
                                    .append("by", -1)))
                    )
            ),

            context -> new Document("$addFields",
                new Document("isRegression",
                    new Document("$or", List.of(
                        new Document("$gte", List.of("$risk_score", "$prevRisk")),
                        new Document("$gt", List.of(
                            new Document("$size", "$vulnerabilities"),
                            "$prevVulnCount"))
                    ))
                )
            ),

            Aggregation.group("package_name").count().as("totalVersions").sum(ConditionalOperators.when("isRegression")
                    .then(1).otherwise(0)).as("regressions").avg("risk_score").as("avgRisk").last("risk_score").as("latestRisk"),

            Aggregation.match(Criteria.where("regressions").gte(2)),

            Aggregation.sort(Sort.by(Sort.Direction.DESC, "latestRisk", "regressions")),
            Aggregation.limit(limit),

            Aggregation.project()
                .and("_id").as("packageName")
                .andExclude("_id")
                .andInclude("totalVersions", "regressions", "avgRisk", "latestRisk")
        );

        return mongoTemplate.aggregate(aggregation,"package",PackageRiskTrendDTO.class).getMappedResults();
    }
}
