package it.unipi.riskDeV.DAO;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Sort;

import it.unipi.riskDeV.DTO.admin.AggreagationPackageDTO;
import it.unipi.riskDeV.DTO.admin.ContributorCountDTO;

@Repository
public class ProjectDAO {
    private MongoTemplate mongoTemplate;

    public List<AggreagationPackageDTO> mostUsedPackages(int limit) {
        
        Aggregation aggregation = Aggregation.newAggregation(
            Aggregation.group("packages.name", "packages.version").count().as("count"),
            Aggregation.sort(Sort.by(Sort.Direction.DESC, "count")),
            Aggregation.limit(limit),
            Aggregation.project()
                            .and("_id.name").as("name")
                            .and("_id.version").as("version")
                            .and("count").as("count")
        );

        AggregationResults<AggreagationPackageDTO> results = mongoTemplate.aggregate(aggregation, "project", AggreagationPackageDTO.class);
        return results.getMappedResults();
    }

    public List<AggreagationPackageDTO> mostUsedPackagesLastMonth(int limit) {
        LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);

        Criteria recentCriteria = Criteria.where("last_update").gte(oneMonthAgo);
        
        Aggregation aggregation = Aggregation.newAggregation(
            Aggregation.match(recentCriteria),
            Aggregation.group("packages.name", "packages.version").count().as("count"),
            Aggregation.sort(Sort.by(Sort.Direction.DESC, "count")),
            Aggregation.limit(limit),
            Aggregation.project()
                            .and("_id.name").as("name")
                            .and("_id.version").as("version")
                            .and("count").as("count")
        );

        AggregationResults<AggreagationPackageDTO> results = mongoTemplate.aggregate(aggregation, "project", AggreagationPackageDTO.class);
        return results.getMappedResults();
    }

    public List<ContributorCountDTO> getTopAdminsLastMonth() {
        LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);
        Criteria recentCriteria = Criteria.where("last_update").gte(oneMonthAgo);

        Aggregation aggregation = Aggregation.newAggregation(
            Aggregation.match(recentCriteria),
            Aggregation.group("admin.username").count().as("count"),
            Aggregation.project().and("_id").as("username").and("count").as("count")
        );

        AggregationResults<ContributorCountDTO> results = mongoTemplate.aggregate(aggregation, "project", ContributorCountDTO.class);

        return results.getMappedResults();
    }


    public List<ContributorCountDTO> getTopCollaboratorsLastMonth() {
        LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);
        Criteria recentCriteria = Criteria.where("last_update").gte(oneMonthAgo);

        Aggregation aggregation = Aggregation.newAggregation(
            Aggregation.match(recentCriteria),
            Aggregation.unwind("collaborators"),
            Aggregation.group("collaborators.username").count().as("count"),
            Aggregation.project().and("_id").as("username").and("count").as("count")
        );

        AggregationResults<ContributorCountDTO> results = mongoTemplate.aggregate(aggregation, "project", ContributorCountDTO.class);

        return results.getMappedResults();
    }
}
