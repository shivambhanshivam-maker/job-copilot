package com.shivam.jobcopilot.controller;

import com.shivam.jobcopilot.service.GmailOAuthService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/gmail")
public class GmailController {

    private final GmailOAuthService gmailOAuthService;

    public GmailController(GmailOAuthService gmailOAuthService) {
        this.gmailOAuthService = gmailOAuthService;
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
            response.sendRedirect("http://localhost:4200/settings?gmail=error");
            return;
        }
        response.sendRedirect("http://localhost:4200/settings?gmail=connected");
    }

    @GetMapping("/status")
    public Map<String, Boolean> status(Authentication auth) {
        UUID userId = (UUID) auth.getPrincipal();
        return Map.of("connected", gmailOAuthService.isConnected(userId));
    }
}
