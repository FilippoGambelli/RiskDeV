package it.unipi.riskDeV.repository.documentDB;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import it.unipi.riskDeV.model.documentDB.User;


public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByUsername(String username);
        
    void deleteByUsername(String username);
    
    boolean existsByEmail(String email);

    boolean existsByUsername(String username);
}
