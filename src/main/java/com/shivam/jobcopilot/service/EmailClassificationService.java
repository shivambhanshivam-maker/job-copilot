package com.shivam.jobcopilot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shivam.jobcopilot.dto.JobApplicationEmail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class EmailClassificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailClassificationService.class);

    private final ChatClient chatClient;
    private final RoleCategoryService roleCategoryService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public EmailClassificationService(ChatClient.Builder chatClientBuilder, RoleCategoryService roleCategoryService) {
        this.roleCategoryService = roleCategoryService;
        this.chatClient = chatClientBuilder
                .defaultSystem("""
                        You are an email classification and extraction assistant.
                        You will receive an email's subject, body, and sender address.

                        Step 1 — Classification:
                        Determine if this email is part of a specific, active or completed job application \
                        process involving the recipient. This includes: referrals, application confirmations, \
                        interview invitations or updates, assessments, recruiter outreach for an ongoing process, \
                        offers, or rejections.
                        Exclude: job alerts, job recommendations, saved job notifications, newsletters, \
                        marketing emails, or messages encouraging the recipient to apply for a role.

                        Step 2 — If job-application related, extract these fields:
                        - company
                        - job_title: extract the full role title exactly as stated in the email, including any \
                        product name, team, or geographic suffix (e.g. "TikTok Shop - Strategy Manager, Strategy & Analytics, EMEA"). \
                        Do not simplify or truncate it.
                        - recruiter_name
                        - recruiter_email (if not found in body, use the sender address)
                        - application_status (one of: Referral Received, Applied, Interview, Offer, Closed, Rejected, Other). \
                        Use "Referral Received" when the email indicates a referral was made but the recipient has not yet submitted an application.
                        - referral (one of: "Yes", "No", or "N/A". Set to "Yes" if the email indicates the application \
                        was submitted through an employee referral, a referral link, or mentions a referring employee. \
                        Set to "No" if the application was clearly direct with no referral. Set to "N/A" if unclear.)
                        - role_category: classify the role into EXACTLY one of the valid categories provided in the user message. \
                        If there is not enough information to confidently classify, use "Other". \
                        Return only the category name, exactly as provided.
                        - interview_date_and_time (if mentioned; use strict ISO-8601 format yyyy-MM-ddTHH:mm:ss, \
                        e.g. "2026-02-22T20:00:00". If only a date is mentioned, use T00:00:00. Always use 24-hour time. \
                        Do NOT include timezone suffixes or AM/PM.)
                        - update_summary: a single natural-language sentence describing what happened in this email, \
                        as if writing a timeline entry. Examples: \
                        "Application submitted for Senior Product Manager role at Stripe.", \
                        "Interview scheduled with Sarah (HR) for March 15th at 3PM.", \
                        "Application rejected after final round — feedback cited lack of fintech domain experience."

                        Respond with ONLY a valid JSON object, no markdown, no extra text.
                        If the email IS job-application related:
                        {"is_job_related": true, "company": "...", "job_title": "...", "recruiter_name": "...", \
                        "recruiter_email": "...", "application_status": "...", "referral": "...", \
                        "role_category": "...", "interview_date_and_time": "...", "update_summary": "..."}
                        Use null for any field you cannot determine.

                        If the email is NOT job-application related:
                        {"is_job_related": false}
                        """)
                .build();
    }

    public JobApplicationEmail classify(String subject, String body, String senderAddress, String gmailMessageId) {
        String categories = String.join(", ", roleCategoryService.getAllNames());
        String prompt = "Subject: " + subject + "\nFrom: " + senderAddress + "\nBody:\n" + body
                + "\n\nValid role categories: " + categories;

        String response = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        try {
            JsonNode json = objectMapper.readTree(response);

            if (!json.path("is_job_related").asBoolean(false)) {
                return null;
            }

            JobApplicationEmail result = new JobApplicationEmail(
                    getTextOrNull(json, "company"),
                    getTextOrNull(json, "job_title"),
                    getTextOrNull(json, "recruiter_name"),
                    getTextOrNull(json, "recruiter_email"),
                    getTextOrNull(json, "application_status"),
                    getTextOrNull(json, "referral"),
                    getTextOrNull(json, "role_category"),
                    getTextOrNull(json, "interview_date_and_time"),
                    gmailMessageId,
                    getTextOrNull(json, "update_summary")
            );
            log.info("Classified email — company: {}, title: {}, status: {}, interviewDate: {}, referral: {}",
                    result.company(), result.jobTitle(), result.applicationStatus(),
                    result.interviewDateAndTime(), result.referral());
            return result;
        } catch (Exception e) {
            log.error("Failed to parse LLM response: {}", response, e);
            return null;
        }
    }

    private String getTextOrNull(JsonNode json, String field) {
        JsonNode node = json.get(field);
        if (node == null || node.isNull()) {
            return null;
        }
        return node.asText();
    }
}