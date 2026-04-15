package com.shivam.jobcopilot.service;

import com.microsoft.aad.msal4j.*;
import com.shivam.jobcopilot.entity.UserOutlookToken;
import com.shivam.jobcopilot.repository.UserOutlookTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class OutlookOAuthService {

    private static final Logger log = LoggerFactory.getLogger(OutlookOAuthService.class);
    private static final String AUTHORITY = "https://login.microsoftonline.com/common";
    private static final Set<String> SCOPES = Set.of(
            "https://graph.microsoft.com/Mail.Read",
            "offline_access"
    );

    @Value("${outlook.client.id}")
    private String clientId;

    @Value("${outlook.client.secret}")
    private String clientSecret;

    @Value("${outlook.oauth.redirect-uri}")
    private String redirectUri;

    private final UserOutlookTokenRepository tokenRepository;
    private final TokenEncryptionService encryptionService;

    public OutlookOAuthService(UserOutlookTokenRepository tokenRepository,
                               TokenEncryptionService encryptionService) {
        this.tokenRepository = tokenRepository;
        this.encryptionService = encryptionService;
    }

    public String buildAuthorizationUrl(UUID userId) throws Exception {
        IConfidentialClientApplication msalApp = buildMsalApp(null);
        AuthorizationRequestUrlParameters params = AuthorizationRequestUrlParameters
                .builder(redirectUri, SCOPES)
                .responseMode(ResponseMode.QUERY)
                .state(userId.toString())
                .build();
        return msalApp.getAuthorizationRequestUrl(params).toString();
    }

    public void handleCallback(String code, String state) throws Exception {
        UUID userId = UUID.fromString(state);

        TokenCacheAspect cacheAspect = new TokenCacheAspect(null);
        IConfidentialClientApplication msalApp = buildMsalApp(cacheAspect);

        AuthorizationCodeParameters params = AuthorizationCodeParameters
                .builder(code, new URI(redirectUri))
                .scopes(SCOPES)
                .build();

        IAuthenticationResult result = msalApp.acquireToken(params).get();

        UserOutlookToken token = tokenRepository.findByUserId(userId).orElseGet(UserOutlookToken::new);
        token.setUserId(userId);
        token.setTokenCacheData(encryptionService.encrypt(cacheAspect.getCacheData()));
        token.setCachedAccessToken(encryptionService.encrypt(result.accessToken()));
        token.setExpiresAtEpochMs(result.expiresOnDate().getTime());
        token.setOutlookAddress(result.account().username());
        tokenRepository.save(token);
        log.info("Outlook token saved for user {} ({})", userId, result.account().username());
    }

    public String getAccessTokenForUser(UUID userId) throws Exception {
        UserOutlookToken stored = tokenRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("No Outlook token for user: " + userId));

        // If token is still valid (not expiring within 5 minutes), decrypt and return directly
        if (stored.getExpiresAtEpochMs() != null
                && System.currentTimeMillis() < stored.getExpiresAtEpochMs() - 300_000) {
            return encryptionService.decrypt(stored.getCachedAccessToken());
        }

        // Token expired or expiring soon — use MSAL to refresh
        String decryptedCache = encryptionService.decrypt(stored.getTokenCacheData());
        TokenCacheAspect cacheAspect = new TokenCacheAspect(decryptedCache);
        IConfidentialClientApplication msalApp = buildMsalApp(cacheAspect);

        IAccount account = msalApp.getAccounts().get().stream().findFirst()
                .orElseThrow(() -> new RuntimeException("No account in MSAL cache for user: " + userId));

        SilentParameters silentParams = SilentParameters.builder(SCOPES, account).build();
        IAuthenticationResult result = msalApp.acquireTokenSilently(silentParams).get();

        // Persist updated cache and cache the access token for fast path
        stored.setTokenCacheData(encryptionService.encrypt(cacheAspect.getCacheData()));
        stored.setCachedAccessToken(encryptionService.encrypt(result.accessToken()));
        stored.setExpiresAtEpochMs(result.expiresOnDate().getTime());
        tokenRepository.save(stored);
        log.info("Outlook token refreshed and saved for user {}", userId);

        return result.accessToken();
    }

    public boolean isConnected(UUID userId) {
        return tokenRepository.findByUserId(userId).isPresent();
    }

    private IConfidentialClientApplication buildMsalApp(TokenCacheAspect cacheAspect) throws Exception {
        IClientCredential credential = ClientCredentialFactory.createFromSecret(clientSecret);
        ConfidentialClientApplication.Builder builder = ConfidentialClientApplication
                .builder(clientId, credential)
                .authority(AUTHORITY);
        if (cacheAspect != null) {
            builder.setTokenCacheAccessAspect(cacheAspect);
        }
        return builder.build();
    }

    private static class TokenCacheAspect implements ITokenCacheAccessAspect {
        private final AtomicReference<String> cacheData;
        private boolean cacheChanged = false;

        TokenCacheAspect(String initialData) {
            this.cacheData = new AtomicReference<>(initialData);
        }

        @Override
        public void beforeCacheAccess(ITokenCacheAccessContext context) {
            String data = cacheData.get();
            if (data != null) {
                context.tokenCache().deserialize(data);
            }
        }

        @Override
        public void afterCacheAccess(ITokenCacheAccessContext context) {
            if (context.hasCacheChanged()) {
                cacheData.set(context.tokenCache().serialize());
                cacheChanged = true;
            }
        }

        String getCacheData() { return cacheData.get(); }
        boolean hasCacheChanged() { return cacheChanged; }
    }
}