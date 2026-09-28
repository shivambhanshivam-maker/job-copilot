package com.shivam.jobcopilot.controller;

import com.shivam.jobcopilot.repository.UserOutlookTokenRepository;
import com.shivam.jobcopilot.dto.OutlookConnectionStatusResponse;
import com.shivam.jobcopilot.entity.OutlookConnectionStatus;
import com.shivam.jobcopilot.entity.UserOutlookToken;
import com.shivam.jobcopilot.service.OutlookOAuthService;
import com.shivam.jobcopilot.service.OutlookService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/outlook")
public class OutlookController {

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    private final OutlookOAuthService outlookOAuthService;
    private final OutlookService outlookService;
    private final UserOutlookTokenRepository outlookTokenRepository;

    public OutlookController(OutlookOAuthService outlookOAuthService,
                             OutlookService outlookService,
                             UserOutlookTokenRepository outlookTokenRepository) {
        this.outlookOAuthService = outlookOAuthService;
        this.outlookService = outlookService;
        this.outlookTokenRepository = outlookTokenRepository;
    }

    @GetMapping("/connect")
    public Map<String, String> connect(Authentication auth) throws Exception {
        UUID userId = (UUID) auth.getPrincipal();
        String url = outlookOAuthService.buildAuthorizationUrl(userId);
        return Map.of("url", url);
    }

    @GetMapping("/callback")
    public void callback(@RequestParam String code,
                         @RequestParam String state,
                         HttpServletResponse response) throws IOException {
        try {
            outlookOAuthService.handleCallback(code, state);
        } catch (Exception e) {
            response.sendRedirect(frontendUrl + "/settings?outlook=error");
            return;
        }
        response.sendRedirect(frontendUrl + "/settings?outlook=connected");
    }

    @GetMapping("/status")
    public OutlookConnectionStatusResponse status(Authentication auth) {
        UUID userId = (UUID) auth.getPrincipal();
        UserOutlookToken token = outlookOAuthService.getToken(userId);
        if (token == null) {
            return new OutlookConnectionStatusResponse(false, "DISCONNECTED", null, null);
        }
        OutlookConnectionStatus status = token.getStatus() == null
                ? OutlookConnectionStatus.CONNECTED
                : token.getStatus();
        return new OutlookConnectionStatusResponse(
                status == OutlookConnectionStatus.CONNECTED,
                status.name(),
                token.getLastError(),
                token.getLastSuccessfulPollAt()
        );
    }

    @PostMapping("/sync")
    public OutlookConnectionStatusResponse sync(Authentication auth) {
        UUID userId = (UUID) auth.getPrincipal();
        outlookService.pollNow(userId);
        return status(auth);
    }

    @PostMapping("/disconnect")
    public ResponseEntity<Void> disconnect(Authentication auth) {
        UUID userId = (UUID) auth.getPrincipal();
        outlookTokenRepository.deleteByUserId(userId);
        return ResponseEntity.noContent().build();
    }
}
