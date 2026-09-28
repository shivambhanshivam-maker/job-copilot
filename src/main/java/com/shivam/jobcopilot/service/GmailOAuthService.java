package com.shivam.jobcopilot.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.shivam.jobcopilot.entity.UserGmailToken;
import com.shivam.jobcopilot.entity.GmailConnectionStatus;
import com.shivam.jobcopilot.exception.GmailReauthenticationRequiredException;
import com.shivam.jobcopilot.repository.UserGmailTokenRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.time.LocalDateTime;

@Service
public class GmailOAuthService {

    private static final Logger log = LoggerFactory.getLogger(GmailOAuthService.class);
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = List.of(com.google.api.services.gmail.GmailScopes.GMAIL_READONLY);

    @Value("${gmail.credentials.path:/credentials.json}")
    private String credentialsPath;

    @Value("${gmail.credentials.json:}")
    private String credentialsJsonBase64;

    @Value("${gmail.oauth.redirect-uri}")
    private String redirectUri;

    private final UserGmailTokenRepository tokenRepository;
    private final TokenEncryptionService encryptionService;

    private GoogleClientSecrets clientSecrets;

    public GmailOAuthService(UserGmailTokenRepository tokenRepository,
                             TokenEncryptionService encryptionService) {
        this.tokenRepository = tokenRepository;
        this.encryptionService = encryptionService;
    }

    @PostConstruct
    public void init() {
        try {
            java.io.Reader reader;
            if (credentialsJsonBase64 != null && !credentialsJsonBase64.isBlank()) {
                byte[] decoded = Base64.getDecoder().decode(credentialsJsonBase64);
                reader = new StringReader(new String(decoded, StandardCharsets.UTF_8));
                log.info("Gmail OAuth credentials loaded from environment variable");
            } else {
                InputStream in = getClass().getResourceAsStream(credentialsPath);
                if (in == null) {
                    log.error("Credentials file not found at {}", credentialsPath);
                    return;
                }
                reader = new InputStreamReader(in);
                log.info("Gmail OAuth credentials loaded from file: {}", credentialsPath);
            }
            clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, reader);
        } catch (Exception e) {
            log.error("Failed to load Gmail credentials: {}", e.getMessage(), e);
        }
    }

    public String buildAuthorizationUrl(UUID userId) throws GeneralSecurityException, IOException {
        NetHttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                transport, JSON_FACTORY, clientSecrets, SCOPES)
                .setAccessType("offline")
                .build();

        var authorizationUrl = flow.newAuthorizationUrl()
                .setRedirectUri(redirectUri)
                .setState(userId.toString());
        authorizationUrl.set("prompt", "consent");
        return authorizationUrl.build();
    }

    public void handleCallback(String code, String state) throws GeneralSecurityException, IOException {
        UUID userId = UUID.fromString(state);
        NetHttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                transport, JSON_FACTORY, clientSecrets, SCOPES)
                .setAccessType("offline")
                .build();

        GoogleTokenResponse tokenResponse = flow.newTokenRequest(code)
                .setRedirectUri(redirectUri)
                .execute();

        UserGmailToken token = tokenRepository.findByUserId(userId)
                .orElseGet(UserGmailToken::new);
        token.setUserId(userId);
        token.setAccessToken(encryptionService.encrypt(tokenResponse.getAccessToken()));
        boolean refreshTokenProvided = tokenResponse.getRefreshToken() != null
                && !tokenResponse.getRefreshToken().isBlank();
        if (refreshTokenProvided) {
            token.setRefreshToken(encryptionService.encrypt(tokenResponse.getRefreshToken()));
        }
        token.setExpiresAtEpochMs(tokenResponse.getExpiresInSeconds() != null
                ? System.currentTimeMillis() + tokenResponse.getExpiresInSeconds() * 1000
                : null);
        if (token.getRefreshToken() == null || token.getRefreshToken().isBlank()) {
            token.setStatus(GmailConnectionStatus.REAUTH_REQUIRED);
            token.setLastError("Gmail did not provide a refresh token; reconnect with consent");
            tokenRepository.save(token);
            log.warn("Gmail OAuth callback completed without a refresh token for user {}", userId);
            throw new GmailReauthenticationRequiredException("Gmail did not provide a refresh token");
        }
        token.setStatus(GmailConnectionStatus.CONNECTED);
        token.setLastError(null);
        tokenRepository.save(token);
        log.info("Gmail token saved for user {} (refresh token provided: {})", userId, refreshTokenProvided);
    }

    @SuppressWarnings("deprecation")
    public Credential getCredentialForUser(UUID userId) throws GeneralSecurityException, IOException {
        UserGmailToken token = tokenRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("No Gmail token for user: " + userId));

        if (token.getRefreshToken() == null || token.getRefreshToken().isBlank()) {
            markReauthenticationRequired(userId, "Gmail did not provide a refresh token");
            throw new GmailReauthenticationRequiredException("Gmail reauthorization is required");
        }

        NetHttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();

        GoogleCredential credential = new GoogleCredential.Builder()
                .setTransport(transport)
                .setJsonFactory(JSON_FACTORY)
                .setClientSecrets(clientSecrets)
                .build();
        credential.setAccessToken(encryptionService.decrypt(token.getAccessToken()));
        credential.setRefreshToken(token.getRefreshToken() != null
                ? encryptionService.decrypt(token.getRefreshToken()) : null);
        if (token.getExpiresAtEpochMs() != null) {
            credential.setExpirationTimeMilliseconds(token.getExpiresAtEpochMs());
        }

        // Auto-refresh if expired or expiring soon
        if (credential.getExpiresInSeconds() != null && credential.getExpiresInSeconds() <= 60) {
            try {
                boolean refreshed = credential.refreshToken();
                if (!refreshed || credential.getAccessToken() == null) {
                    markReauthenticationRequired(userId, "Gmail refresh token was rejected");
                    throw new GmailReauthenticationRequiredException("Gmail reauthorization is required");
                }
                saveRefreshedAccessToken(token, credential);
            } catch (TokenResponseException e) {
                if (e.getStatusCode() == 400 || e.getStatusCode() == 401) {
                    markReauthenticationRequired(userId, "Gmail refresh token is no longer valid");
                    throw new GmailReauthenticationRequiredException("Gmail reauthorization is required", e);
                }
                throw e;
            }
        }

        return credential;
    }

    public boolean isConnected(UUID userId) {
        return tokenRepository.findByUserId(userId)
                .map(token -> token.getStatus() == null || token.getStatus() == GmailConnectionStatus.CONNECTED)
                .orElse(false);
    }

    public UserGmailToken getToken(UUID userId) {
        return tokenRepository.findByUserId(userId).orElse(null);
    }

    public void markPollSuccess(UUID userId, long lastPollEpochSeconds) {
        tokenRepository.findByUserId(userId).ifPresent(token -> {
            token.setStatus(GmailConnectionStatus.CONNECTED);
            token.setLastPollEpochSeconds(lastPollEpochSeconds);
            token.setLastSuccessfulPollAt(LocalDateTime.now());
            token.setLastError(null);
            tokenRepository.save(token);
        });
    }

    public void markReauthenticationRequired(UUID userId, String message) {
        updateConnectionState(userId, GmailConnectionStatus.REAUTH_REQUIRED, message);
    }

    public void markError(UUID userId, String message) {
        updateConnectionState(userId, GmailConnectionStatus.ERROR, message);
    }

    private void updateConnectionState(UUID userId, GmailConnectionStatus status, String message) {
        tokenRepository.findByUserId(userId).ifPresent(token -> {
            token.setStatus(status);
            token.setLastError(message);
            tokenRepository.save(token);
        });
    }

    private void saveRefreshedAccessToken(UserGmailToken token, Credential credential) {
        token.setAccessToken(encryptionService.encrypt(credential.getAccessToken()));
        token.setExpiresAtEpochMs(credential.getExpirationTimeMilliseconds());
        token.setStatus(GmailConnectionStatus.CONNECTED);
        token.setLastError(null);
        tokenRepository.save(token);
    }
}
