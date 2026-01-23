package it.unipi.riskDeV.service;

import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import it.unipi.riskDeV.DTO.UpdateProfileDTO;
import it.unipi.riskDeV.DTO.UserDTO;
import it.unipi.riskDeV.common.DomainError;
import it.unipi.riskDeV.common.Result;
import it.unipi.riskDeV.event.UserDeletedEvent;
import it.unipi.riskDeV.event.UserUpdatedEvent;
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

    public Result<UserDTO> getProfile(String id) {

        var optUser = userRepository.findById(id);
        if (optUser.isEmpty()) {
            return new Result.Failure<>(new DomainError.NotFound("User not found"));
        }

        var user = optUser.get();
        return new Result.Success<>(UserDTO.fromEntity(user));
    }

    public Result<List<String>> getUserProjectNames(String userId) {
        var optUser = userRepository.findById(userId);
        if (optUser.isEmpty()) {
            return new Result.Failure<>(new DomainError.NotFound("User not found"));
        }

        var user = optUser.get();
        List<String> projectNames = user.getProjectNames() != null ? user.getProjectNames() : List.of();

        return new Result.Success<>(projectNames);
    }

    public Result<UserDTO> updateProfile(String userId, UpdateProfileDTO request) {
    
        var userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return new Result.Failure<>(new DomainError.NotFound("User not found"));
        }
        
        User user = userOpt.get();
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
                    eventPublisher.publishEvent(new UserUpdatedEvent(savedUser.getId(), savedUser.getUsername()));
                }

                return new Result.Success<>(UserDTO.fromEntity(savedUser));

            } catch (Exception e) {
                return new Result.Failure<>(new DomainError.SystemError("Error updating profile", e));
            }
        }

        return new Result.Success<>(UserDTO.fromEntity(user));
    }

    public Result<Void> deleteUser(String id) {
        
        if (!userRepository.existsById(id)) {
            return new Result.Failure<>(new DomainError.NotFound("User not found"));
        }

        try {
            userRepository.deleteById(id); 
            eventPublisher.publishEvent(new UserDeletedEvent(id));
            return new Result.Success<>();

        } catch (Exception e) {
            log.error("Error deleting user {}: {}", id, e.getMessage());
            return new Result.Failure<>(new DomainError.SystemError("Failed to delete user from database", e));
        }

    }

}
