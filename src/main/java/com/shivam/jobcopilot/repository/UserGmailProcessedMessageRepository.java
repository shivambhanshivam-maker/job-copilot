package com.shivam.jobcopilot.repository;

import com.shivam.jobcopilot.entity.UserGmailProcessedMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserGmailProcessedMessageRepository extends JpaRepository<UserGmailProcessedMessage, UUID> {
    boolean existsByUserIdAndGmailMessageId(UUID userId, String gmailMessageId);
}
