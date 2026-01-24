package it.unipi.riskDeV.service;

import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import it.unipi.riskDeV.DTO.UpdateProfileDTO;
import it.unipi.riskDeV.DTO.UserDTO;
import it.unipi.riskDeV.common.DomainError;
import it.unipi.riskDeV.common.Result;
import it.unipi.riskDeV.event.UserEvent;
import it.unipi.riskDeV.model.User;
import it.unipi.riskDeV.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final PasswordEncoder passwordEncoder;
    private final MongoTemplate mongoTemplate;

    public Result<UserDTO> getProfile(String id) {
        log.info("Get user profile");
        
        var optUser = userRepository.findById(id);
        if (optUser.isEmpty()) {
            return new Result.Failure<>(new DomainError.NotFound("User not found"));
        }

        User user = optUser.get();
        return new Result.Success<>(UserDTO.fromEntity(user));
    }

    public Result<UserDTO> getProfileByUsername(String username) {
        log.info("Get user profile by username");

        var optUser = userRepository.findByUsername(username);
        if (optUser.isEmpty()) {
            return new Result.Failure<>(new DomainError.NotFound("User not found"));
        }

        User user = optUser.get();
        return new Result.Success<>(UserDTO.fromEntity(user));
    }

    public Result<List<String>> getUserProjectNames(String userId) {
        log.info("Get user projects");

        var optUser = userRepository.findById(userId);
        if (optUser.isEmpty()) {
            return new Result.Failure<>(new DomainError.NotFound("User not found"));
        }

        User user = optUser.get();
        List<String> projectNames = user.getProjectNames() != null ? user.getProjectNames() : List.of();

        return new Result.Success<>(projectNames);
    }

    public Result<UserDTO> updateProfile(String userId, UpdateProfileDTO request) {
        log.info("Updating use profile");

        var userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return new Result.Failure<>(new DomainError.NotFound("User not found"));
        }
        
        User user = userOpt.get();
        String oldUsername = user.getUsername();
        boolean isUpdated = false;
        boolean usernameChanged = false;

        if (request.username() != null && !request.username().isBlank()) {
            // Check if username is changing
            if (!request.username().equals(user.getUsername())) {
                
                // Check uniqueness
                if (userRepository.existsByUsername(request.username())) {
                    return new Result.Failure<>(new DomainError.AlreadyExists("Username already taken"));
                }

                user.setUsername(request.username());
                isUpdated = true;
                usernameChanged = true;
            }
        }

        if (request.firstName() != null && !request.firstName().isBlank()) {
            user.setFirstName(request.firstName());
            isUpdated = true;
        }
        if (request.lastName() != null && !request.lastName().isBlank()) {
            user.setLastName(request.lastName());
            isUpdated = true;
        }
        if (request.password() != null && !request.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.password()));
            isUpdated = true;
        }

        if (isUpdated) {
            try {
                User savedUser = userRepository.save(user);

                // If username changed, publish event
                if (usernameChanged) {
                    eventPublisher.publishEvent(new UserEvent.UserUpdatedEvent(oldUsername, savedUser.getUsername()));
                }

                return new Result.Success<>(UserDTO.fromEntity(savedUser));

            } catch (Exception e) {
                return new Result.Failure<>(new DomainError.SystemError("Error updating profile", e));
            }
        }

        log.info("User profile updated");
        return new Result.Success<>(UserDTO.fromEntity(user));
    }

    public Result<String> deleteUser(String id) {
        log.info("Deleting user profile");
        if (!userRepository.existsById(id)) {
            return new Result.Failure<>(new DomainError.NotFound("User not found"));
        }

        try {
            userRepository.deleteById(id);
            log.info("User profile deleted");

            eventPublisher.publishEvent(new UserEvent.UserDeletedEvent(id));
            return new Result.Success<>("User profile deleted");

        } catch (Exception e) {
            return new Result.Failure<>(new DomainError.SystemError("Failed to delete user from database", e));
        }

    }

    public void addProjectToUser(String username, String projectName) {
        log.info("Adding project '{}' to user '{}'", projectName, username);
        
        Query query = Query.query(Criteria.where("username").is(username));
        
        Update update = new Update().addToSet("project_names", projectName);
    
        mongoTemplate.updateFirst(query, update, User.class);
    }
    
    public void removeProjectFromUser(String username, String projectName) {
        log.info("Removing project '{}' from user '{}'", projectName, username);
        
        Query query = Query.query(Criteria.where("username").is(username));
        Update update = new Update().pull("project_names", projectName);
        
        mongoTemplate.updateFirst(query, update, User.class);
    }

}
