package it.unipi.riskDeV.util;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import it.unipi.riskDeV.DTO.GeneralPackageDTO;
import it.unipi.riskDeV.model.PackageVersion;
import it.unipi.riskDeV.model.PackageVersion.Constraints;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class Helper {
    private final Utility util;
    private final MongoTemplate mongoTemplate;

    public List<PackageVersion> addDependeciesGraph(String packageName, String version, List<Constraints> dependenciesList) {
        List<PackageVersion> result = new ArrayList<>();
        
        for (Constraints constraint : dependenciesList) {
            
            Criteria criteria = Criteria.where("package_name").is(constraint.getName());

            if (constraint.getVersion_gte() != null) {
                criteria = criteria.and("version_array").gte(util.generateVersionArray(constraint.getVersion_gte()));
            }
            if (constraint.getVersion_lte() != null) {
                criteria = criteria.and("version_array").lte(util.generateVersionArray(constraint.getVersion_lte()));
            }
            if (constraint.getVersion_gt() != null) {
                criteria = criteria.and("version_array").gt(util.generateVersionArray(constraint.getVersion_gt()));
            }
            if (constraint.getVersion_lt() != null) {
                criteria = criteria.and("version_array").lt(util.generateVersionArray(constraint.getVersion_lt()));
            }
            if (constraint.getVersion_eq() != null) {
                criteria = criteria.and("version_array").is(util.generateVersionArray(constraint.getVersion_eq()));
            }
            if (constraint.getVersion_neq() != null) {
                criteria = criteria.and("version_array").ne(util.generateVersionArray(constraint.getVersion_neq()));
            }

            Query query = new Query(criteria);

            result.addAll(mongoTemplate.find(query, PackageVersion.class));
        }

        return result;
    }

    public List<PackageVersion> updateDependeciesGraph(String packageName, String version) {
        Criteria dependencyCriteria = Criteria.where("requires_dist").elemMatch(
            Criteria.where("name").is(packageName)
                    .orOperator(
                        Criteria.where("version_gte").lte(version),
                        Criteria.where("version_gte").is(null)
                    )
                    .orOperator(
                        Criteria.where("version_lte").gte(version),
                        Criteria.where("version_lte").is(null)
                    )
                    .orOperator(
                        Criteria.where("version_gt").lt(version),
                        Criteria.where("version_gt").is(null)
                    )
                    .orOperator(
                        Criteria.where("version_lt").gt(version),
                        Criteria.where("version_lt").is(null)
                    )
                    .orOperator(
                        Criteria.where("version_eq").is(version),
                        Criteria.where("version_eq").is(null)
                    )
                    .orOperator(
                        Criteria.where("version_neq").ne(version),
                        Criteria.where("version_neq").is(null)
                    )
            );

        Query query = new Query(dependencyCriteria);
        
        return mongoTemplate.find(query, PackageVersion.class);
    }

    public void updatePackageGeneralMetadata(String packageName, GeneralPackageDTO updateData) {
        Query query = new Query(Criteria.where("package_name").is(packageName));
        Update update = new Update()
                .set("author", updateData.getAuthor())
                .set("author_email", updateData.getAuthorEmail())
                .set("description", updateData.getDescription())
                .set("package_url", updateData.getPackageURL())
                .set("documentation", updateData.getDocumentationURL());

        mongoTemplate.updateMulti(query, update, PackageVersion.class);
    }
}
