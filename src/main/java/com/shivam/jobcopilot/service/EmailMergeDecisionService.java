package com.shivam.jobcopilot.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shivam.jobcopilot.dto.JobApplicationEmail;
import com.shivam.jobcopilot.dto.MergeDecision;
import com.shivam.jobcopilot.entity.FitAnalysis;
import com.shivam.jobcopilot.entity.JobApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class EmailMergeDecisionService {

    private static final Logger log = LoggerFactory.getLogger(EmailMergeDecisionService.class);

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public EmailMergeDecisionService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder
                .defaultSystem("""
                        You are a job application data manager.

                        You will receive:
                        1. Extracted data from a job-application email.
                        2. A list of existing job applications at the same company, each with their current state \
                        and recent update history.
                        3. A list of existing fit analyses at the same company (may be empty).

                        Your task: decide what to do with the email data.

                        Actions:
                        - UPDATE: the email is an update to one of the existing applications. Specify which one via applicationId.
                        - CREATE: this email represents a brand-new application not yet tracked.
                        - IGNORE: the email is clearly already reflected in the existing data (true duplicate).

                        Matching rules (apply in order):
                        1. If the email contains a job title that matches an existing application's title — prefer that application.
                        2. If the email status is terminal or advancing (Rejected, Offer, Closed), only consider \
                        applications in active stages (Applied, Interview, Referral Received). \
                        Do not match a rejection to an already-closed or already-rejected application.
                        3. If still ambiguous, prefer the application most recently updated.
                        4. If no existing application is a reasonable match, use CREATE.

                        Fit analysis linking rules:
                        - Review the fit analyses list and check if any refers to the same role as this email. \
                        Title matching should be semantic and fuzzy — minor differences in wording, punctuation, or \
                        abbreviation should still match (e.g. "Product Operations Manager (Cards)" matches \
                        "Product Operations Manager, Cards Division"). \
                        - If a match is found, return its fitAnalysisId. Return null if no match.

                        Field update rules:
                        - "fieldsToSet": the ONLY way to set a value. Include only fields the email explicitly provides \
                        a real, non-empty value for. Do NOT include a field just because it was absent in the email — \
                        omitting it preserves the existing stored value, which is correct. \
                        Never put null or empty string here. \
                        For "roleCategory": do not include it if the extracted value is "Other" and the existing \
                        application already has a real category — "Other" means unknown, not a real update.
                        - "fieldsToClear": the ONLY way to intentionally erase a value. Use this when a field is \
                        no longer relevant due to a status transition — it is a deliberate decision, not an absence. \
                        Example: put "interviewDate" here when the new status is Offer, Rejected, or Closed, \
                        because the interview is over and the stored date is now stale and misleading. \
                        Do NOT put a field here just because the email didn't mention it.
                        - Never clear "company" or "jobTitle".
                        - Valid field names: company, jobTitle, recruiterName, recruiterEmail, applicationStatus, \
                        referral, roleCategory, interviewDate.

                        Respond with ONLY a valid JSON object — no markdown, no extra text:
                        {"action": "UPDATE|CREATE|IGNORE", "applicationId": "existing UUID or null", \
                        "fitAnalysisId": "matching fit analysis UUID or null", \
                        "fieldsToSet": {"fieldName": "value"}, "fieldsToClear": ["fieldName"], \
                        "reasoning": "one sentence explaining your decision"}
                        """)
                .build();
    }

    public MergeDecision decide(JobApplicationEmail email, List<JobApplication> candidates,
                                List<FitAnalysis> fitAnalyses) {
        try {
            Map<String, Object> emailMap = new LinkedHashMap<>();
            emailMap.put("company", nvl(email.company()));
            emailMap.put("jobTitle", nvl(email.jobTitle()));
            emailMap.put("recruiterName", nvl(email.recruiterName()));
            emailMap.put("recruiterEmail", nvl(email.recruiterEmail()));
            emailMap.put("applicationStatus", nvl(email.applicationStatus()));
            emailMap.put("referral", nvl(email.referral()));
            emailMap.put("roleCategory", nvl(email.roleCategory()));
            emailMap.put("interviewDate", nvl(email.interviewDateAndTime()));
            emailMap.put("updateSummary", nvl(email.updateSummary()));

            List<Map<String, Object>> candidateMaps = candidates.stream().map(app -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("applicationId", app.getId().toString());
                m.put("jobTitle", nvl(app.getJobTitle()));
                m.put("applicationStatus", nvl(app.getApplicationStatus()));
                m.put("recruiterName", nvl(app.getRecruiterName()));
                m.put("recruiterEmail", nvl(app.getRecruiterEmail()));
                m.put("referral", nvl(app.getReferral()));
                m.put("roleCategory", nvl(app.getRoleCategory()));
                m.put("interviewDate", app.getInterviewDate() != null ? app.getInterviewDate().toString() : null);
                m.put("updatedAt", app.getUpdatedAt() != null ? app.getUpdatedAt().toString() : null);
                List<String> recentUpdates = app.getUpdates().stream()
                        .skip(Math.max(0, app.getUpdates().size() - 3))
                        .map(u -> u.getSummary())
                        .collect(Collectors.toList());
                m.put("recentUpdates", recentUpdates);
                return m;
            }).collect(Collectors.toList());

            List<Map<String, Object>> fitAnalysisMaps = fitAnalyses.stream().map(fa -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("fitAnalysisId", fa.getId().toString());
                m.put("jobTitle", nvl(fa.getJobTitle()));
                m.put("fitScore", fa.getFitScore());
                return m;
            }).collect(Collectors.toList());

            String prompt = "Extracted email data:\n" + objectMapper.writeValueAsString(emailMap)
                    + "\n\nExisting applications at this company:\n" + objectMapper.writeValueAsString(candidateMaps)
                    + "\n\nExisting fit analyses at this company:\n" + objectMapper.writeValueAsString(fitAnalysisMaps);

            String response = chatClient.prompt().user(prompt).call().content();
            log.debug("Merge decision response: {}", response);

            JsonNode json = objectMapper.readTree(response);

            String action = json.path("action").asText("CREATE");
            UUID applicationId = parseUuid(json, "applicationId");
            UUID fitAnalysisId = parseUuid(json, "fitAnalysisId");

            Map<String, String> fieldsToSet = new HashMap<>();
            JsonNode fieldsNode = json.path("fieldsToSet");
            if (fieldsNode.isObject()) {
                fieldsToSet = objectMapper.convertValue(fieldsNode, new TypeReference<>() {});
            }

            List<String> fieldsToClear = new ArrayList<>();
            JsonNode clearNode = json.path("fieldsToClear");
            if (clearNode.isArray()) {
                fieldsToClear = objectMapper.convertValue(clearNode, new TypeReference<>() {});
            }

            String reasoning = json.path("reasoning").asText("");
            log.info("Merge decision — action: {}, appId: {}, fitAnalysisId: {}, reasoning: {}",
                    action, applicationId, fitAnalysisId, reasoning);

            return new MergeDecision(action, applicationId, fitAnalysisId, fieldsToSet, fieldsToClear, reasoning);

        } catch (Exception e) {
            log.error("Merge decision failed, falling back to CREATE: {}", e.getMessage(), e);
            return new MergeDecision("CREATE", null, null, null, null, "fallback due to error");
        }
    }

    private UUID parseUuid(JsonNode json, String field) {
        JsonNode node = json.path(field);
        if (node.isNull() || node.isMissingNode()) return null;
        String val = node.asText("").trim();
        if (val.isBlank() || val.equalsIgnoreCase("null")) return null;
        try { return UUID.fromString(val); } catch (IllegalArgumentException ignored) { return null; }
    }

    private String nvl(String s) {
        return s != null ? s : "";
    }
}