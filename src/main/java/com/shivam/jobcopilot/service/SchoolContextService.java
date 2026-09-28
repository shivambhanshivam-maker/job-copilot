package com.shivam.jobcopilot.service;

import com.shivam.jobcopilot.dto.SchoolContextResponse;
import com.shivam.jobcopilot.entity.Advisor;
import com.shivam.jobcopilot.entity.School;
import com.shivam.jobcopilot.repository.AdvisorRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class SchoolContextService {

    private final AdvisorRepository advisorRepository;

    public SchoolContextService(AdvisorRepository advisorRepository) {
        this.advisorRepository = advisorRepository;
    }

    @Transactional(readOnly = true)
    public SchoolContextResponse getContext(UUID userId) {
        Advisor advisor = advisorRepository.findByUserIdAndIsActiveTrue(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "No active advisor access"));

        School school = advisor.getSchool();
        return new SchoolContextResponse(
                advisor.getId(),
                advisor.getName(),
                advisor.getEmail(),
                advisor.getTitle(),
                advisor.getRole(),
                school.getId(),
                school.getName(),
                school.getSlug(),
                school.getDomain()
        );
    }
}
