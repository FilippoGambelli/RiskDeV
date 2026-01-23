package it.unipi.riskDeV.repository;

import it.unipi.riskDeV.model.neo4j.ProjectNode;

import java.util.List;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectGraphRepository extends Neo4jRepository<ProjectNode, String> {

    @Query("MERGE (p:Project {name: $projectName})")
    void createProjectNode(@Param("projectName") String projectName);

    @Query("""
        MATCH (p:Project {name: $projectName})
        MERGE (u:User {mongoId: $userId}) 
        MERGE (u)-[:OWNS]->(p)
    """)
    void setProjectOwner(@Param("projectName") String projectName, @Param("userId") String userId);

    @Query("MATCH (p:Project {name: $projectName}) DETACH DELETE p")
    void deleteProjectByName(@Param("projectName") String projectName);

    @Query("""
        MATCH (p:Project {name: $projectName})
        OPTIONAL MATCH (p)-[r:USES]->(:Version)
        DELETE r
        WITH p
        UNWIND $packageIds as pkgId
        MERGE (v:Version {id: pkgId})
        MERGE (p)-[:USES]->(v)
    """)
    void replaceAllDependencies(@Param("projectName") String projectName, @Param("packageIds") List<String> packageIds);

    @Query("""
        MATCH (p:Project {name: $projectName})
        MERGE (u:User {mongoId: $userId})
        MERGE (u)-[:WORKS_ON]->(p)
    """)
    void addCollaboratorRelation(@Param("projectName") String projectName, @Param("userId") String userId);

    @Query("""
        MATCH (u:User {mongoId: $userId})-[r:WORKS_ON]->(p:Project {name: $projectName})
        DELETE r
    """)
    void removeCollaboratorRelation(@Param("projectName") String projectName, @Param("userId") String userId);

}
    