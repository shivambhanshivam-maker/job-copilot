package com.shivam.jobcopilot.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDate;

@Service
public class ChatbotService {

    private static final String SYSTEM_PROMPT = """
            You are a helpful job search assistant for a Job Copilot application.
            You have access to the user's job applications, fit analyses, and discovered job listings via tools.

            Use the available tools to answer questions about:
            - Job application status, history, and stats
            - Fit analysis results for specific companies and roles
            - Pending actions that need attention
            - Jobs discovered by the nightly scheduler

            Be concise and actionable. When listing items, keep it brief.
            Today's date is %s.
            """.formatted(LocalDate.now());

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;

    public ChatbotService(ChatClient.Builder chatClientBuilder, ChatbotTools chatbotTools) {
        // MessageWindowChatMemory auto-creates an InMemoryChatMemoryRepository when none is provided
        this.chatMemory = MessageWindowChatMemory.builder().build();
        this.chatClient = chatClientBuilder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultTools(chatbotTools)
                .build();
    }

    public Flux<String> chat(String message, String conversationId) {
        return chatClient.prompt()
                .user(message)
                // Advisor created per-request so each conversation ID gets its own history window
                .advisors(MessageChatMemoryAdvisor.builder(chatMemory)
                        .conversationId(conversationId)
                        .build())
                .stream()
                .content();
    }
}