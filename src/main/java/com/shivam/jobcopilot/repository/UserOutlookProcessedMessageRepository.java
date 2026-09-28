package com.shivam.jobcopilot.repository;

import com.shivam.jobcopilot.entity.UserOutlookProcessedMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserOutlookProcessedMessageRepository extends JpaRepository<UserOutlookProcessedMessage, UUID> {
    boolean existsByUserIdAndOutlookMessageId(UUID userId, String outlookMessageId);
}
