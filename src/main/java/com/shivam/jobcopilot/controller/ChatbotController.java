package com.shivam.jobcopilot.controller;

import com.shivam.jobcopilot.service.ChatbotService;
import com.shivam.jobcopilot.service.ChatbotTools;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/chat")
public class ChatbotController {

    private final ChatbotService chatbotService;

    public ChatbotController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    // Request body: { "message": "...", "conversationId": "<uuid>" }
    // conversationId is optional — a new one is generated if omitted (starts a fresh conversation)
    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@RequestBody Map<String, String> request, Authentication auth) {
        String message = request.get("message");
        String conversationId = request.getOrDefault("conversationId", UUID.randomUUID().toString());
        UUID userId = (UUID) auth.getPrincipal();

        SecurityContext securityContext = SecurityContextHolder.getContext();
        ChatbotTools.setUserId(userId);
        try {
            return chatbotService.chat(message, conversationId)
                    .doFinally(signal -> ChatbotTools.clearUserId())
                    .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext)));
        } catch (Exception e) {
            ChatbotTools.clearUserId();
            throw e;
        }
    }
}
