package com.shivam.jobcopilot.service;

import com.shivam.jobcopilot.entity.CV;
import com.shivam.jobcopilot.repository.CVRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CVService {

    private final CVRepository cvRepository;

    public CVService(CVRepository cvRepository) {
        this.cvRepository = cvRepository;
    }

    public CV save(CV cv, UUID userId) {
        cv.setUserId(userId);
        return cvRepository.save(cv);
    }

    public List<CV> listAll(UUID userId) {
        return cvRepository.findByUserId(userId);
    }

    public CV getById(UUID id) {
        return cvRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("CV not found with id: " + id));
    }

    @Transactional
    public CV setDefault(UUID id, UUID userId) {
        cvRepository.findByUserIdAndIsDefaultCvTrue(userId).ifPresent(cv -> {
            cv.setDefaultCv(false);
            cvRepository.save(cv);
        });

        CV cv = cvRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("CV not found with id: " + id));
        cv.setDefaultCv(true);
        return cvRepository.save(cv);
    }

    public CV getDefault(UUID userId) {
        return cvRepository.findByUserIdAndIsDefaultCvTrue(userId)
                .orElseThrow(() -> new RuntimeException("No default CV set"));
    }
}
