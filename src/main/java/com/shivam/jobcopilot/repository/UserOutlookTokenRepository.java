package com.shivam.jobcopilot.repository;

import com.shivam.jobcopilot.entity.UserOutlookToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserOutlookTokenRepository extends JpaRepository<UserOutlookToken, UUID> {
    Optional<UserOutlookToken> findByUserId(UUID userId);
    List<UserOutlookToken> findAll();

    @Transactional
    void deleteByUserId(UUID userId);
}