package it.unipi.riskDeV.service;

import org.springframework.stereotype.Service;

import it.unipi.riskDeV.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import it.unipi.riskDeV.common.DomainError;
import it.unipi.riskDeV.common.Result;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {
    private final UserRepository userRepository;

    public Result<String> addNewAdmin(String username) {
        
        var optionalUser = userRepository.findByUsername(username);

        if (optionalUser.isEmpty()) {
            return new Result.Failure<>(
                new DomainError.NotFound("User " + username + " does not exist")
            );
        }

        var user = optionalUser.get();

        if (user.getRole().equals("ROLE_ADMIN")) {
            return new Result.Failure<>(
                new DomainError.AlreadyExists("User " + username + " is already an administrator")
            );
        }

        user.setRole("ROLE_ADMIN");

        try {
            userRepository.save(user);
            log.info("Administrator {} was successfully added", username);
        } catch (Exception e) {
            log.warn("Failed to save administrator with username {}", username);
            // TODO: handle persistence error properly
        }

        return new Result.Success<>("Administrator was successfully added");
    }

    public Result<String> removeAdmin(String username) {
        
        var optionalUser = userRepository.findByUsername(username);

        if (optionalUser.isEmpty()) {
            return new Result.Failure<>(
                new DomainError.NotFound("User " + username + " does not exist")
            );
        }

        var user = optionalUser.get();
        
        if (user.getRole().equals("ROLE_USER")) {
            return new Result.Failure<>(
                new DomainError.AlreadyExists("User " + username + " is already a standard user")
            );
        }

        user.setRole("ROLE_USER");

        try {
            userRepository.save(user);
            log.info("Administrator {} was successfully removed", username);
        } catch (Exception e) {
            log.warn("Failed to remove administrator with username {}", username);
            // TODO: handle persistence error properly
        }

        return new Result.Success<>("Administrator was successfully removed");
    }
}