package it.unipi.riskDeV.service;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import it.unipi.riskDeV.repository.ProjectGraphRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectService {

    private final ProjectGraphRepository projectGraphRepository;

    public List<String> getAllUserProjects() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userId = auth.getName();

        return projectGraphRepository.findProjectIdsByUserId("emily.smith");
        // return projectGraphRepository.findProjectIdsByUserId(userId);
    }
}
