package it.unipi.riskDeV.service;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

import it.unipi.riskDeV.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import it.unipi.riskDeV.exception.ServiceException;
import it.unipi.riskDeV.model.User;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {
    private final UserRepository userRepository;

    public void addNewAdmin(String username) {
        User user = userRepository.findById(username)
            .orElseThrow(() -> new BadCredentialsException("Invalid username"));

        if(user.getRole().equals("ROLE_ADMIN")) {
            throw new ServiceException("This user is alredy an administrator");
        }

        user.setRole("ROLE_ADMIN");
        try {
            userRepository.save(user);
            log.info("Administrator {} added correctly", username);
        } catch (Exception e) {
            throw new ServiceException("Error adding " + username + " as administrator");
        }
    }

    public void removeAdmin(String username) {
        User user = userRepository.findById(username)
            .orElseThrow(() -> new BadCredentialsException("Invalid username"));

        if(user.getRole().equals("ROLE_USER")) {
            throw new ServiceException("This user is alredy a standard user");
        }

        user.setRole("ROLE_USER");
        try {
            userRepository.save(user);
            log.info("Administrator {} removed correctly", username);
        } catch (Exception e) {
            throw new ServiceException("Error removing " + username + " as administrator");
        }
    }
}
