package com.shivam.jobcopilot.repository;

import com.shivam.jobcopilot.entity.AllowedEmail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AllowedEmailRepository extends JpaRepository<AllowedEmail, Long> {
    boolean existsByEmail(String email);
}