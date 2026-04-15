package com.shivam.jobcopilot.service;

import com.microsoft.graph.models.Message;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.MessageCollectionPage;
import com.shivam.jobcopilot.dto.JobApplicationEmail;
import com.shivam.jobcopilot.entity.UserOutlookToken;
import com.shivam.jobcopilot.repository.UserOutlookTokenRepository;
import com.microsoft.graph.options.HeaderOption;
import okhttp3.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;

@Service
public class OutlookService {

    private static final Logger log = LoggerFactory.getLogger(OutlookService.class);

    @Value("${outlook.poll.max-results:20}")
    private int maxResults;

    private final EmailClassificationService classificationService;
    private final JobApplicationService jobApplicationService;
    private final OutlookOAuthService outlookOAuthService;
    private final UserOutlookTokenRepository tokenRepository;

    // Per-user state
    private final Map<UUID, Set<String>> processedMessageIds = new ConcurrentHashMap<>();
    private final Map<UUID, String> lastPollDateTime = new ConcurrentHashMap<>();

    public OutlookService(EmailClassificationService classificationService,
                          JobApplicationService jobApplicationService,
                          OutlookOAuthService outlookOAuthService,
                          UserOutlookTokenRepository tokenRepository) {
        this.classificationService = classificationService;
        this.jobApplicationService = jobApplicationService;
        this.outlookOAuthService = outlookOAuthService;
        this.tokenRepository = tokenRepository;
    }

    @Scheduled(fixedRateString = "${outlook.poll.interval:60000}")
    public void pollAllUsers() {
        List<UserOutlookToken> tokens = tokenRepository.findAll();
        if (tokens.isEmpty()) {
            log.info("No Outlook accounts connected, skipping poll");
            return;
        }
        for (UserOutlookToken token : tokens) {
            try {
                pollForUser(token.getUserId());
            } catch (Exception e) {
                String message = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                if (message.contains("401") || message.contains("403")) {
                    log.warn("Auth failure for Outlook user {}, disconnecting", token.getUserId());
                    tokenRepository.deleteByUserId(token.getUserId());
                    processedMessageIds.remove(token.getUserId());
                    lastPollDateTime.remove(token.getUserId());
                } else {
                    log.error("Error polling Outlook for user {}: {}", token.getUserId(), message, e);
                }
            }
        }
    }

    private void pollForUser(UUID userId) throws Exception {
        String accessToken = outlookOAuthService.getAccessTokenForUser(userId);
        String pollStart = OffsetDateTime.now().withNano(0).toString();
        String since = lastPollDateTime.getOrDefault(userId,
                OffsetDateTime.now().minusMinutes(5).withNano(0).toString());

        Set<String> processed = processedMessageIds.computeIfAbsent(userId, k -> Collections.synchronizedSet(new HashSet<>()));

        GraphServiceClient<Request> graphClient = GraphServiceClient
                .builder()
                .authenticationProvider(requestUrl -> CompletableFuture.completedFuture(accessToken))
                .buildClient();

        MessageCollectionPage page = graphClient.me().messages()
                .buildRequest(List.of(new HeaderOption("Prefer", "outlook.body-content-type=\"text\"")))
                .filter("receivedDateTime ge " + since)
                .top(maxResults)
                .select("id,subject,from,body,receivedDateTime")
                .get();

        List<Message> messages = page.getCurrentPage();
        if (messages.isEmpty()) {
            log.info("No new Outlook messages for user {} since last poll", userId);
            lastPollDateTime.put(userId, pollStart);
            return;
        }

        for (Message msg : messages) {
            String messageId = msg.id;
            if (processed.contains(messageId)) continue;

            String subject = msg.subject != null ? msg.subject : "";
            String from = (msg.from != null && msg.from.emailAddress != null)
                    ? msg.from.emailAddress.address : "";
            String body = (msg.body != null) ? msg.body.content : "";

            log.info("Processing Outlook email for user {}: '{}' from {}", userId, subject, from);

            JobApplicationEmail result = classificationService.classify(subject, body, from, messageId);
            processed.add(messageId);

            if (result != null) {
                jobApplicationService.upsert(result, userId);
                log.info("Job email detected for user {}: {} at {} — status: {}",
                        userId, result.jobTitle(), result.company(), result.applicationStatus());
            } else {
                log.info("Outlook email not job-related for user {}, skipping: '{}'", userId, subject);
            }
        }
        lastPollDateTime.put(userId, pollStart);
    }
}