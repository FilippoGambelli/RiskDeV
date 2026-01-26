package it.unipi.riskDeV.DAO;

import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.stereotype.Repository;

import it.unipi.riskDeV.DTO.admin.RiskAggregationDTO;

@Repository
public class PackageDAO {

    @Autowired
    private MongoTemplate mongoTemplate;

    public RiskAggregationDTO aggregateRiskBuckets() {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.sort(Sort.by(Sort.Direction.DESC, "version_array")),
                Aggregation.group("package_name").first("package_name").as("package_name")
                    .first("risk_score").as("risk_score"),
                Aggregation.bucket("risk_score").withBoundaries(0, 2, 4, 6, 8, 10)
                    .withDefaultBucket("Other").andOutputCount().as("count"),
                Aggregation.group().sum("count").as("total_packages")
                    .push(new Document("risk_interval", "$_id").append("count", "$count")).as("buckets"),
                Aggregation.project().andExclude("_id")
                    .andInclude("total_packages", "buckets")
        );

        AggregationResults<RiskAggregationDTO> result = mongoTemplate.aggregate(aggregation, "package", RiskAggregationDTO.class);

        return result.getUniqueMappedResult();
    }
}