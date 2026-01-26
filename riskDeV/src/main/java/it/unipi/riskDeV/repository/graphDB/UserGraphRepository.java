package it.unipi.riskDeV.repository.graphDB;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

import it.unipi.riskDeV.model.graphDB.UserNode;

public interface UserGraphRepository extends Neo4jRepository<UserNode, String> {
    
    @Query("MERGE (u:User {username: $username}) RETURN u")
    void createUserNode(@Param("username") String username);

    @Query("MATCH (u:User {username: $oldUsername}) SET u.username = $newUsername")
    void updateUsername(@Param("oldUsername") String oldUsername, @Param("newUsername") String newUsername);
}
