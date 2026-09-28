package com.shivam.jobcopilot.controller;

import com.shivam.jobcopilot.repository.UserGmailTokenRepository;
import com.shivam.jobcopilot.dto.GmailConnectionStatusResponse;
import com.shivam.jobcopilot.entity.GmailConnectionStatus;
import com.shivam.jobcopilot.entity.UserGmailToken;
import com.shivam.jobcopilot.service.GmailOAuthService;
import com.shivam.jobcopilot.service.GmailService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/gmail")
public class GmailController {

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    private final GmailOAuthService gmailOAuthService;
    private final GmailService gmailService;
    private final UserGmailTokenRepository gmailTokenRepository;

    public GmailController(GmailOAuthService gmailOAuthService,
                           GmailService gmailService,
                           UserGmailTokenRepository gmailTokenRepository) {
        this.gmailOAuthService = gmailOAuthService;
        this.gmailService = gmailService;
        this.gmailTokenRepository = gmailTokenRepository;
    }

    @GetMapping("/connect")
    public Map<String, String> connect(Authentication auth) throws Exception {
        UUID userId = (UUID) auth.getPrincipal();
        String url = gmailOAuthService.buildAuthorizationUrl(userId);
        return Map.of("url", url);
    }

    @GetMapping("/callback")
    public void callback(@RequestParam String code,
                         @RequestParam String state,
                         HttpServletResponse response) throws IOException {
        try {
            gmailOAuthService.handleCallback(code, state);
        } catch (Exception e) {
            response.sendRedirect(frontendUrl + "/settings?gmail=error");
            return;
        }
        response.sendRedirect(frontendUrl + "/settings?gmail=connected");
    }

    @GetMapping("/status")
    public GmailConnectionStatusResponse status(Authentication auth) {
        UUID userId = (UUID) auth.getPrincipal();
        UserGmailToken token = gmailOAuthService.getToken(userId);
        if (token == null) {
            return new GmailConnectionStatusResponse(false, "DISCONNECTED", null, null);
        }
        GmailConnectionStatus status = token.getStatus() == null
                ? GmailConnectionStatus.CONNECTED
                : token.getStatus();
        return new GmailConnectionStatusResponse(
                status == GmailConnectionStatus.CONNECTED,
                status.name(),
                token.getLastError(),
                token.getLastSuccessfulPollAt()
        );
    }

    @PostMapping("/sync")
    public GmailConnectionStatusResponse sync(Authentication auth) {
        UUID userId = (UUID) auth.getPrincipal();
        gmailService.pollNow(userId);
        return status(auth);
    }

    @PostMapping("/disconnect")
    public ResponseEntity<Void> disconnect(Authentication auth) {
        UUID userId = (UUID) auth.getPrincipal();
        gmailTokenRepository.deleteByUserId(userId);
        return ResponseEntity.noContent().build();
    }
}
