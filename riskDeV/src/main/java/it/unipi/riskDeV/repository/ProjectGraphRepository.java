package it.unipi.riskDeV.repository;

import it.unipi.riskDeV.model.neo4j.ProjectNode;

import java.time.Instant;
import java.util.List;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectGraphRepository extends Neo4jRepository<ProjectNode, String> {

    @Query("""
        MATCH (u:User {id: $userId})-[r:OWNS]->(p:Project {id: $projectId})
        RETURN count(r) > 0
    """)
    boolean isUserOwner(@Param("userId") String userId, @Param("projectId") String projectId);

    @Query("""
        MATCH (u:User {id: $userId})-[:OWNS|WORKS_ON]->(p:Project)
        RETURN DISTINCT p.id AS id
    """)
    List<String> findProjectIdsByUserId(@Param("userId") String userId);

    @Query("""
        MATCH (p:Project {id: $projectId})
        MERGE (u:User {id: $collaboratorId})
        MERGE (u)-[:WORKS_ON]->(p)
    """)
    void addCollaborator(@Param("projectId") String projectId, @Param("collaboratorId") String collaboratorId);

    @Query("""
        MATCH (u:User {id: $collaboratorId})-[r:WORKS_ON]->(p:Project {id: $projectId})
        DELETE r
    """)
    void removeCollaborator(@Param("projectId") String projectId, @Param("collaboratorId") String collaboratorId);

    @Query("""
        MATCH (u:User)-[:WORKS_ON]->(p:Project {id: $projectId})
        RETURN u.id
    """)
    List<String> findCollaboratorsByProjectId(@Param("projectId") String projectId);

    @Query("""
        MATCH (p:Project {id: $projectId})-[r:USES]->(v:Version)
        WHERE v.id IN $packageNames
        DELETE r
    """)
    void removeDependenciesByName(@Param("projectId") String projectId, @Param("packageNames") List<String> packageNames);

    @Query("""
        MATCH (p:Project {id: $projectId})
        SET p.lastUpdate = $newDate
    """)
    void updateLastUpdateTimestamp(@Param("projectId") String projectId, @Param("newDate") Instant newDate);

    @Query("""
        MATCH (p:Project {id: $projectId})
        UNWIND $versionIds AS vid
        MERGE (v:Version {id: vid}) 
        MERGE (p)-[:USES]->(v)
    """)
    void addDependenciesToProject(@Param("projectId") String projectId, @Param("versionIds") List<String> versionIds);

}
    