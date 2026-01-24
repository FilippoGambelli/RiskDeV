package it.unipi.riskDeV.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import it.unipi.riskDeV.model.User;


public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByUsername(String username);
        
    boolean existsByEmail(String email);

    boolean existsByUsername(String username);
}
