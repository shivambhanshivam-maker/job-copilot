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
                        interview invitations or updates, offers, rejections, recruiter outreach for an ongoing \
                        process, and assessments — but ONLY if the assessment is sent in the context of a \
                        specific job application at a named employer (e.g. "complete this test as the next step \
                        in your application at Stripe"). The assessment may be delivered via a third-party \
                        platform (HackerRank, Alooba, Codility, etc.) but it must clearly relate to an active \
                        hiring process for a specific role at a specific company.
                        Exclude: job alerts, job recommendations, saved job notifications, newsletters, \
                        marketing emails, messages encouraging the recipient to apply for a role, and any \
                        assessment or exercise that does NOT reference a specific job application — including \
                        practice tests, free trials, "learning and development" exercises, and emails sent by \
                        the assessment platform itself to promote their product or invite the recipient to try \
                        their service. If no specific employer or role is identifiable as the requester, \
                        it is not job-application related.

                        Step 2 — If job-application related, extract these fields:
                         - company_name_raw: the employer name exactly as identified in the email or sender context. Preserve meaningful brand words.
                         - company_name_canonical: the employer identity used for matching applications across workflows. Remove only clear legal-entity suffixes such as Inc, LLC, Ltd, Limited, Corp, Corporation, GmbH, Pvt Ltd, S.A., or PLC. Do not remove meaningful brand words: "Bain & Company" must remain "Bain & Company", not "Bain". If uncertain, use the supplied employer name.
                         Use the raw value for display and the canonical value for matching.
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
                         {"is_job_related": true, "company_name_raw": "...", "company_name_canonical": "...", "job_title": "...", "recruiter_name": "...", \
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

            String companyRaw = firstTextOrNull(json, "company_name_raw", "company");
            String companyCanonical = firstTextOrNull(json, "company_name_canonical");
            if (companyCanonical == null) companyCanonical = companyRaw;

            String recruiterEmail = getTextOrNull(json, "recruiter_email");
            if (isBlank(recruiterEmail) && !isBlank(senderAddress)) {
                recruiterEmail = senderAddress.trim();
            }

            String referral = getTextOrNull(json, "referral");
            if (isBlank(referral)) referral = "N/A";

            String roleCategory = getTextOrNull(json, "role_category");
            if (isBlank(roleCategory)) roleCategory = "Other";

            JobApplicationEmail result = new JobApplicationEmail(
                    companyRaw,
                    companyCanonical,
                    getTextOrNull(json, "job_title"),
                    getTextOrNull(json, "recruiter_name"),
                    recruiterEmail,
                    getTextOrNull(json, "application_status"),
                    referral,
                    roleCategory,
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

    private String firstTextOrNull(JsonNode json, String... fields) {
        for (String field : fields) {
            String value = getTextOrNull(json, field);
            if (value != null && !value.isBlank()) return value;
        }
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
