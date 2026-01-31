package it.unipi.riskDeV.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.unipi.riskDeV.DTO.MessageResponseDTO;
import it.unipi.riskDeV.DTO.user.UpdateProfileDTO;
import it.unipi.riskDeV.DTO.user.UserDTO;
import it.unipi.riskDeV.async.events.UserEvent;
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
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    
    public Result<UserDTO> getProfile(String username) {
        log.info("Get user profile");
        
        var optUser = userRepository.findByUsername(username);
        if (optUser.isEmpty()) {
            return new Result.Failure<>(new DomainError.NotFound("User not found"));
        }

        User user = optUser.get();
        return new Result.Success<>(new UserDTO(user));
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

    @Transactional
    public Result<UserDTO> updateProfile(String username, UpdateProfileDTO request) {
        var userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return new Result.Failure<>(new DomainError.NotFound("User not found"));
        }
        
        User user = userOpt.get();
        boolean importantChanges = false;
        boolean isUpdated = false;

        if (request.getUsername() != null && !request.getUsername().isBlank() && 
            !request.getUsername().equals(user.getUsername())) {

            if (userRepository.existsByUsername(request.getUsername())) {
                return new Result.Failure<>(new DomainError.AlreadyExists("Username already taken"));
            }

            user.setUsername(request.getUsername());
            isUpdated = true;
            importantChanges = true;
        }

        if (request.getEmail() != null && !request.getEmail().isBlank() && 
            !request.getEmail().equals(user.getEmail())) {
            
            if (userRepository.existsByEmail(request.getEmail())) {
                return new Result.Failure<>(new DomainError.AlreadyExists("Email already taken"));
            }
            
            user.setEmail(request.getEmail());
            isUpdated = true;
            importantChanges = true;
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

        if (!isUpdated) {
            return new Result.Success<>(new UserDTO(user));
        }

        User savedUser = userRepository.save(user);

        if (importantChanges && savedUser.getProjectNames() != null && !savedUser.getProjectNames().isEmpty()) {
            eventPublisher.publishEvent(new UserEvent.UserUpdated(savedUser.getProjectNames(), username, savedUser.getUsername(), savedUser.getEmail()));
        }

        return new Result.Success<>(new UserDTO(savedUser));
    }

    @Transactional
    public Result<MessageResponseDTO> deleteUser(String username) {
        var userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return new Result.Failure<>(new DomainError.NotFound("User not found."));
        }
        
        User user = userOpt.get();
        if (!user.getProjectNames().isEmpty()) {
            return new Result.Failure<>(new DomainError.InvalidOperation("Quit from all your projects before delete your account."));
        }

        userRepository.deleteByUsername(username);
        return new Result.Success<>(new MessageResponseDTO("User profile deleted."));
    }

    @Transactional
    public Result<Void> addProjectToUser(String username, String projectName) {
        var userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return new Result.Failure<>(new DomainError.NotFound("User " + username + " not found"));
        }
        
        User user = userOpt.get();
        
        if (user.getProjectNames() == null) {
            user.setProjectNames(new ArrayList<>());
        }
        
        if (user.getProjectNames().contains(projectName)) {
            return new Result.Success<>(null);
        }

        user.getProjectNames().add(projectName);
        
        userRepository.save(user);
        return new Result.Success<>(null);
    }
    
    @Transactional
    public Result<Void> removeProjectFromUser(String username, String projectName) {
        var userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return new Result.Failure<>(new DomainError.NotFound(String.format("User '%s' not found", username)));
        }
        
        User user = userOpt.get();
        if (user.getProjectNames() != null && user.getProjectNames().remove(projectName)) {
            userRepository.save(user);
        }
        
        return new Result.Success<>(null);
    }

}
