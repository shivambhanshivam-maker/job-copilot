package com.shivam.jobcopilot.service;

import com.microsoft.graph.models.Message;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.MessageCollectionPage;
import com.microsoft.graph.options.HeaderOption;
import com.shivam.jobcopilot.dto.JobApplicationEmail;
import com.shivam.jobcopilot.entity.OutlookConnectionStatus;
import com.shivam.jobcopilot.entity.UserOutlookProcessedMessage;
import com.shivam.jobcopilot.entity.UserOutlookToken;
import com.shivam.jobcopilot.repository.UserOutlookProcessedMessageRepository;
import com.shivam.jobcopilot.repository.UserOutlookTokenRepository;
import okhttp3.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
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
    private final UserOutlookProcessedMessageRepository processedMessageRepository;

    public OutlookService(EmailClassificationService classificationService,
                          JobApplicationService jobApplicationService,
                          OutlookOAuthService outlookOAuthService,
                          UserOutlookTokenRepository tokenRepository,
                          UserOutlookProcessedMessageRepository processedMessageRepository) {
        this.classificationService = classificationService;
        this.jobApplicationService = jobApplicationService;
        this.outlookOAuthService = outlookOAuthService;
        this.tokenRepository = tokenRepository;
        this.processedMessageRepository = processedMessageRepository;
    }

    @Scheduled(fixedRateString = "${outlook.poll.interval:60000}")
    public void pollAllUsers() {
        List<UserOutlookToken> tokens = tokenRepository.findAll();
        if (tokens.isEmpty()) {
            log.info("No Outlook accounts connected, skipping poll");
            return;
        }

        for (UserOutlookToken token : tokens) {
            if (token.getStatus() == OutlookConnectionStatus.REAUTH_REQUIRED) {
                log.debug("Skipping Outlook poll for user {} because reauthorization is required", token.getUserId());
                continue;
            }
            pollSafely(token);
        }
    }

    public void pollNow(UUID userId) {
        tokenRepository.findByUserId(userId).ifPresentOrElse(
                token -> {
                    if (token.getStatus() == OutlookConnectionStatus.REAUTH_REQUIRED) {
                        log.info("Outlook reauthorization is required for user {}, skipping manual sync", userId);
                    } else {
                        pollSafely(token);
                    }
                },
                () -> log.info("No Outlook account connected for user {}, skipping manual sync", userId)
        );
    }

    private void pollSafely(UserOutlookToken token) {
        try {
            pollForUser(token.getUserId());
        } catch (Exception e) {
            if (isAuthenticationFailure(e)) {
                outlookOAuthService.markReauthenticationRequired(
                        token.getUserId(), "Outlook authorization is no longer valid");
                log.warn("Outlook reauthorization required for user {}", token.getUserId());
            } else {
                outlookOAuthService.markError(token.getUserId(), "Outlook polling failed");
                log.error("Error polling Outlook for user {}: {}", token.getUserId(), errorMessage(e), e);
            }
        }
    }

    private void pollForUser(UUID userId) throws Exception {
        String accessToken = outlookOAuthService.getAccessTokenForUser(userId);
        UserOutlookToken storedToken = tokenRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("No Outlook token for user: " + userId));

        long pollStart = Instant.now().getEpochSecond();
        long lastPoll = storedToken.getLastPollEpochSeconds() != null
                ? storedToken.getLastPollEpochSeconds()
                : pollStart - 300;
        String since = OffsetDateTime.ofInstant(Instant.ofEpochSecond(lastPoll), ZoneOffset.UTC)
                .withNano(0)
                .toString();

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

        List<Message> messages = page != null ? page.getCurrentPage() : List.of();
        if (messages == null || messages.isEmpty()) {
            log.info("No new Outlook messages for user {} since last poll", userId);
            outlookOAuthService.markPollSuccess(userId, pollStart);
            return;
        }

        boolean processingFailures = false;
        for (Message msg : messages) {
            String messageId = msg.id;
            if (messageId == null
                    || processedMessageRepository.existsByUserIdAndOutlookMessageId(userId, messageId)) {
                continue;
            }

            try {
                String subject = msg.subject != null ? msg.subject : "";
                String from = (msg.from != null && msg.from.emailAddress != null)
                        ? msg.from.emailAddress.address : "";
                String body = msg.body != null ? msg.body.content : "";

                log.info("Processing Outlook email for user {}: '{}' from {}", userId, subject, from);
                JobApplicationEmail result = classificationService.classify(subject, body, from, messageId);

                if (result != null) {
                    jobApplicationService.upsert(result, userId);
                    log.info("Job email detected for user {}: {} at {} - status: {}",
                            userId, result.jobTitle(), result.company(), result.applicationStatus());
                } else {
                    log.info("Outlook email not job-related for user {}, skipping: '{}'", userId, subject);
                }

                processedMessageRepository.save(new UserOutlookProcessedMessage(userId, messageId));
            } catch (Exception e) {
                processingFailures = true;
                log.error("Could not process Outlook message {} for user {}; it will be retried: {}",
                        messageId, userId, errorMessage(e), e);
            }
        }

        if (processingFailures) {
            outlookOAuthService.markError(userId,
                    "One or more Outlook messages could not be processed; retrying");
        } else {
            outlookOAuthService.markPollSuccess(userId, pollStart);
        }
    }

    private boolean isAuthenticationFailure(Throwable error) {
        Throwable current = error;
        while (current != null) {
            String message = current.getMessage();
            if (message != null) {
                String normalized = message.toLowerCase(Locale.ROOT);
                if (normalized.contains("401")
                        || normalized.contains("403")
                        || normalized.contains("invalid_grant")
                        || normalized.contains("interaction_required")
                        || normalized.contains("aadsts700082")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    private String errorMessage(Throwable error) {
        return error.getMessage() != null ? error.getMessage() : error.getClass().getSimpleName();
    }
}
