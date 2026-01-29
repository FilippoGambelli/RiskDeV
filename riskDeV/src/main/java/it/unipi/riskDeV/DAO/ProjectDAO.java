package it.unipi.riskDeV.DAO;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.ConvertOperators;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;

import it.unipi.riskDeV.DTO.admin.AggreagationPackageDTO;
import it.unipi.riskDeV.DTO.admin.ContributorCountDTO;

@Repository
public class ProjectDAO {

    @Autowired
    private MongoTemplate mongoTemplate;

    public List<AggreagationPackageDTO> mostUsedPackages(int limit) {
    
        Aggregation aggregation = Aggregation.newAggregation(
            Aggregation.unwind("packages"),
            
            Aggregation.group("packages.name", "packages.version").count().as("count"),
            
            Aggregation.sort(Sort.by(Sort.Direction.DESC, "count")),
            
            Aggregation.limit(limit),
            
            Aggregation.project()
                .and("_id.name").as("packageName")
                .and("_id.version").as("version")
                .and("count").as("count")
        );

        AggregationResults<AggreagationPackageDTO> results = mongoTemplate.aggregate(
            aggregation, "project", AggreagationPackageDTO.class
        );
        
        return results.getMappedResults();
    }

    public List<AggreagationPackageDTO> mostUsedPackagesLastMonth(int limit) {
        LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);

        Aggregation aggregation = Aggregation.newAggregation(
            Aggregation.addFields().addFieldWithValue("lastUpdateDate", 
                ConvertOperators.ToDate.toDate("$last_update")).build(),
            
            Aggregation.match(Criteria.where("lastUpdateDate").gte(oneMonthAgo)),
            
            Aggregation.unwind("packages"),
            
            Aggregation.group("packages.name", "packages.version").count().as("count"),
            
            Aggregation.sort(Sort.Direction.DESC, "count"),
            
            Aggregation.limit(limit),
            
            Aggregation.project("count")
                .and("_id.name").as("packageName")
                .and("_id.version").as("version")
                .andExclude("_id")
        );

        return mongoTemplate.aggregate(aggregation, "project", AggreagationPackageDTO.class)
                            .getMappedResults();
    }

    public List<ContributorCountDTO> getTopCollaboratorsLastMonth() {
        LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);

        Aggregation aggregation = Aggregation.newAggregation(
            Aggregation.addFields().addFieldWithValue("lastUpdateDate", 
                ConvertOperators.ToDate.toDate("$last_update")).build(),
            
            Aggregation.match(Criteria.where("lastUpdateDate").gte(oneMonthAgo)),
            
            Aggregation.unwind("collaborators"),
            
            Aggregation.group("collaborators.username").count().as("count"),
            
            Aggregation.project("count")
                .and("_id").as("username")
                .andExclude("_id"),
            
            Aggregation.sort(Sort.Direction.DESC, "count"),
            
            Aggregation.limit(10)
        );

        return mongoTemplate.aggregate(aggregation, "project", ContributorCountDTO.class)
                            .getMappedResults();
    }
}
