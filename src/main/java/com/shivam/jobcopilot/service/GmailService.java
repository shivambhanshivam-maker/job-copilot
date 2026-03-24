package com.shivam.jobcopilot.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartHeader;
import com.shivam.jobcopilot.dto.JobApplicationEmail;
import com.shivam.jobcopilot.entity.UserGmailToken;
import com.shivam.jobcopilot.repository.UserGmailTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GmailService {

    private static final Logger log = LoggerFactory.getLogger(GmailService.class);
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    @Value("${gmail.poll.max-results:20}")
    private long maxResults;

    private final EmailClassificationService classificationService;
    private final JobApplicationService jobApplicationService;
    private final GmailOAuthService gmailOAuthService;
    private final UserGmailTokenRepository tokenRepository;

    // Per-user state
    private final Map<UUID, Set<String>> processedMessageIds = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastPollEpochSeconds = new ConcurrentHashMap<>();

    public GmailService(EmailClassificationService classificationService,
                        JobApplicationService jobApplicationService,
                        GmailOAuthService gmailOAuthService,
                        UserGmailTokenRepository tokenRepository) {
        this.classificationService = classificationService;
        this.jobApplicationService = jobApplicationService;
        this.gmailOAuthService = gmailOAuthService;
        this.tokenRepository = tokenRepository;
    }

    @Scheduled(fixedRateString = "${gmail.poll.interval:60000}")
    public void pollAllUsers() {
        List<UserGmailToken> tokens = tokenRepository.findAll();
        if (tokens.isEmpty()) {
            log.info("No Gmail accounts connected, skipping poll");
            return;
        }
        for (UserGmailToken token : tokens) {
            try {
                pollForUser(token.getUserId());
            } catch (Exception e) {
                log.error("Error polling Gmail for user {}: {}", token.getUserId(), e.getMessage(), e);
            }
        }
    }

    private void pollForUser(UUID userId) throws GeneralSecurityException, IOException {
        Credential credential = gmailOAuthService.getCredentialForUser(userId);
        NetHttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
        Gmail gmailClient = new Gmail.Builder(transport, JSON_FACTORY, credential)
                .setApplicationName("Job Copilot")
                .build();

        long lastPoll = lastPollEpochSeconds.getOrDefault(userId, Instant.now().getEpochSecond());
        long pollStart = Instant.now().getEpochSecond();
        Set<String> processed = processedMessageIds.computeIfAbsent(userId, k -> Collections.synchronizedSet(new HashSet<>()));

        String query = "after:" + lastPoll;
        ListMessagesResponse response = gmailClient.users().messages()
                .list("me").setQ(query).setMaxResults(maxResults).execute();

        List<Message> messages = response.getMessages();
        if (messages == null || messages.isEmpty()) {
            log.info("No new messages for user {} since last poll", userId);
            lastPollEpochSeconds.put(userId, pollStart);
            return;
        }

        for (Message msgRef : messages) {
            if (processed.contains(msgRef.getId())) continue;

            Message fullMessage = gmailClient.users().messages()
                    .get("me", msgRef.getId()).setFormat("full").execute();

            String subject = getHeader(fullMessage, "Subject");
            String from = getHeader(fullMessage, "From");
            String body = extractBody(fullMessage.getPayload());

            log.info("Processing email for user {}: '{}' from {}", userId, subject, from);

            JobApplicationEmail result = classificationService.classify(subject, body, from, msgRef.getId());
            processed.add(msgRef.getId());

            if (result != null) {
                jobApplicationService.upsert(result, userId);
                log.info("Job email detected for user {}: {} at {} — status: {}",
                        userId, result.jobTitle(), result.company(), result.applicationStatus());
            } else {
                log.info("Email not job-related for user {}, skipping: '{}'", userId, subject);
            }
        }
        lastPollEpochSeconds.put(userId, pollStart);
    }

    private String getHeader(Message message, String headerName) {
        if (message.getPayload() == null || message.getPayload().getHeaders() == null) {
            return "";
        }
        return message.getPayload().getHeaders().stream()
                .filter(h -> headerName.equalsIgnoreCase(h.getName()))
                .map(MessagePartHeader::getValue)
                .findFirst()
                .orElse("");
    }

    private String extractBody(MessagePart part) {
        if (part == null) {
            return "";
        }

        // If this part has a plain text body directly
        if (part.getBody() != null && part.getBody().getData() != null
                && "text/plain".equals(part.getMimeType())) {
            return new String(Base64.getUrlDecoder().decode(part.getBody().getData()));
        }

        // Recurse into multipart parts, prefer text/plain
        if (part.getParts() != null) {
            for (MessagePart subPart : part.getParts()) {
                if ("text/plain".equals(subPart.getMimeType())
                        && subPart.getBody() != null && subPart.getBody().getData() != null) {
                    return new String(Base64.getUrlDecoder().decode(subPart.getBody().getData()));
                }
            }
            // Fallback: recurse into first subpart
            for (MessagePart subPart : part.getParts()) {
                String result = extractBody(subPart);
                if (!result.isEmpty()) {
                    return result;
                }
            }
        }

        return "";
    }
}
