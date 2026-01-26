package it.unipi.riskDeV.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import it.unipi.riskDeV.DTO.user.UpdateProfileDTO;
import it.unipi.riskDeV.DTO.user.UserDTO;
import it.unipi.riskDeV.async.events.UserEvents;
import it.unipi.riskDeV.model.documentDB.User;
import it.unipi.riskDeV.repository.documentDB.UserRepository;
import it.unipi.riskDeV.results.DomainError;
import it.unipi.riskDeV.results.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final PasswordEncoder passwordEncoder;

    public Result<UserDTO> getProfile(String username) {
        log.info("Get user profile");
        
        var optUser = userRepository.findByUsername(username);
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

    public Result<List<String>> getUserProjectNames(String username) {
        log.info("Get user projects");

        var optUser = userRepository.findByUsername(username);
        if (optUser.isEmpty()) {
            return new Result.Failure<>(new DomainError.NotFound("User not found"));
        }

        User user = optUser.get();
        List<String> projectNames = user.getProjectNames() != null ? user.getProjectNames() : List.of();

        return new Result.Success<>(projectNames);
    }

    public Result<UserDTO> updateProfile(String username, UpdateProfileDTO request) {
        log.info("Updating use profile");

        var userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return new Result.Failure<>(new DomainError.NotFound("User not found"));
        }
        
        User user = userOpt.get();
        String oldUsername = user.getUsername();
        boolean isUpdated = false;
        boolean usernameChanged = false;

        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            // Check if username is changing
            if (!request.getUsername().equals(user.getUsername())) {

                // Check uniqueness
                if (userRepository.existsByUsername(request.getUsername())) {
                    return new Result.Failure<>(new DomainError.AlreadyExists("Username already taken"));
                }

                user.setUsername(request.getUsername());
                isUpdated = true;
                usernameChanged = true;
            }
        }

        if (request.getFirstName() != null && !request.getFirstName().isBlank()) {
            user.setFirstName(request.getFirstName());
            isUpdated = true;
        }
        if (request.getLastName() != null && !request.getLastName().isBlank()) {
            user.setLastName(request.getLastName());
            isUpdated = true;
        }
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            isUpdated = true;
        }

        if (isUpdated) {
            try {
                User savedUser = userRepository.save(user);

                // If username changed, publish event
                if (usernameChanged) {
                    eventPublisher.publishEvent(new UserEvents.UserUpdatedEvent(oldUsername, savedUser.getUsername()));
                }

                log.info("User profile updated");
                return new Result.Success<>(UserDTO.fromEntity(savedUser));

            } catch (Exception e) {
                return new Result.Failure<>(new DomainError.SystemError("Error updating profile", e));
            }
        }

        log.info("User profile updated");
        return new Result.Success<>(UserDTO.fromEntity(user));
    }

    public Result<String> deleteUser(String username) {
        log.info("Deleting user profile");

        if (!userRepository.existsByUsername(username)) {
            return new Result.Failure<>(new DomainError.NotFound("User not found"));
        }

        try {
            userRepository.deleteByUsername(username);
            log.info("User profile deleted");

            eventPublisher.publishEvent(new UserEvents.UserDeletedEvent(username));
            return new Result.Success<>("User profile deleted");

        } catch (Exception e) {
            return new Result.Failure<>(new DomainError.SystemError("Failed to delete user from database", e));
        }

    }

    public Result<Void> addProjectToUser(String username, String projectName) {
        log.debug("Adding project '{}' to user '{}'", projectName, username);
        
        var userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return new Result.Failure<>(new DomainError.NotFound(String.format("User '%s' not found", username)));
        }
        
        try {
            User user = userOpt.get();
            if (user.getProjectNames() == null) {
                user.setProjectNames(new ArrayList<>());
            }
            
            if (!user.getProjectNames().contains(projectName)) {
                user.getProjectNames().add(projectName);
                userRepository.save(user);
                log.debug("Project '{}' added to user '{}'", projectName, username);
            } else {
                log.debug("Project '{}' already exists for user '{}'", projectName, username);
            }

            return new Result.Success<>(null);
        } catch (Exception e) {
            log.error("Failed to add project '{}' to user '{}'", projectName, username, e);
            return new Result.Failure<>(new DomainError.SystemError(String.format("Failed to add project '%s' to user '%s'", projectName, username), e));
        }
        
    }
    
    public Result<Void> removeProjectFromUser(String username, String projectName) {
        log.debug("Removing project '{}' from user '{}'", projectName, username);
        
        var userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            log.error("User '{}' not found when removing project '{}'", username, projectName);
            return new Result.Failure<>(new DomainError.NotFound(String.format("User '%s' not found", username)));
        }
        
        try {
            User user = userOpt.get();
            if (user.getProjectNames() != null && user.getProjectNames().remove(projectName)) {
                userRepository.save(user);
                log.debug("Project '{}' removed from user '{}'", projectName, username);
            } else {
                log.debug("Project '{}' not found for user '{}'", projectName, username);
            }

            return new Result.Success<>(null);
        } catch (Exception e) {
            log.error("Failed to remove project '{}' from user '{}'", projectName, username, e);
            return new Result.Failure<>(new DomainError.SystemError(String.format("Failed to remove project '%s' from user '%s'", projectName, username), e));
        }
        
    }

}
