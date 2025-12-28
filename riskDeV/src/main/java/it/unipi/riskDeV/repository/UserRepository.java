package it.unipi.riskDeV.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import it.unipi.riskDeV.model.User;

public interface UserRepository extends MongoRepository<User, String> {
    boolean existsByEmail(String email);
}
