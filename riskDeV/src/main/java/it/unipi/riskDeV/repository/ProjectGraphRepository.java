package it.unipi.riskDeV.repository;

import it.unipi.riskDeV.model.neo4j.ProjectNode;
import java.util.List;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectGraphRepository extends Neo4jRepository<ProjectNode, String> {

    @Query("""
        MATCH (u:User {id: $userId})-[:OWNS_PROJECT]->(p:Project)
        RETURN DISTINCT p.id AS id
    """)
    List<String> findProjectIdsByUserId(@Param("userId") String userId);
}
    