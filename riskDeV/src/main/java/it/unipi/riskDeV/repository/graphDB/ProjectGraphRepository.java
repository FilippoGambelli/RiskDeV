package it.unipi.riskDeV.repository.graphDB;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import it.unipi.riskDeV.model.graphDB.ProjectNode;

@Repository
public interface ProjectGraphRepository extends Neo4jRepository<ProjectNode, String> {

    @Query("MERGE (p:Project {name: $projectName})")
    void createProjectNode(@Param("projectName") String projectName);

    @Query("MATCH (p:Project {name: $projectName}) DETACH DELETE p")
    void deleteProjectByName(@Param("projectName") String projectName);

    @Query("""
        MATCH (p:Project {name: $projectName})
        MATCH (v_new:Version {package_name: $packageName, version: $packageVersion})
        MERGE (p)-[:USES]->(v_new)
    """)
    void addDependency(@Param("projectName") String projectName, @Param("packageName") String packageName, @Param("packageVersion") String packageVersion);

    @Query("""
        MATCH (p:Project {name: $projectName}) 
        MATCH (p)-[r:USES]->(v:Version) 
        DELETE r
    """)
    void removeAllDependency(@Param("projectName") String projectName);
}
    