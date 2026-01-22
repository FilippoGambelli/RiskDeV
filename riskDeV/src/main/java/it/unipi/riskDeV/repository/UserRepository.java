package it.unipi.riskDeV.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import it.unipi.riskDeV.model.User;
import java.util.Optional;


public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByUsername(String username);
    
    boolean existsByEmail(String email);
}
