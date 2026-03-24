package com.shivam.jobcopilot.repository;

import com.shivam.jobcopilot.entity.UserGmailToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserGmailTokenRepository extends JpaRepository<UserGmailToken, UUID> {
    Optional<UserGmailToken> findByUserId(UUID userId);
    List<UserGmailToken> findAll();
}
