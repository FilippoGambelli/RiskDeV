package it.unipi.riskDeV.repository;

import it.unipi.riskDeV.model.neo4j.UserNode;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

public interface UserGraphRepository extends Neo4jRepository<UserNode, String> {
    
    @Query("MERGE (u:User {username: $username}) SET u.mongoId = $mongoId RETURN u")
    void createUserNode(@Param("mongoId") String mongoId, @Param("username") String username);

    @Query("MATCH (u:User {mongoId: $mongoId}) SET u.username = $newUsername")
    void updateUsername(@Param("mongoId") String mongoId, @Param("newUsername") String newUsername);
}
