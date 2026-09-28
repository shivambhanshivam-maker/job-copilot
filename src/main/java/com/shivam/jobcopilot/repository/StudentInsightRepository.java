package com.shivam.jobcopilot.repository;

import com.shivam.jobcopilot.entity.StudentInsight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface StudentInsightRepository extends JpaRepository<StudentInsight, UUID> {
    List<StudentInsight> findByUserIdAndStatusOrderByGeneratedAtDesc(UUID userId, String status);

    @Transactional
    void deleteByUserId(UUID userId);
}
