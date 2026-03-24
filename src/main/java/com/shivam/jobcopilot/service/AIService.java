package com.shivam.jobcopilot.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class AIService {

    private final ChatClient chatClient;

    public AIService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder
                .defaultSystem("""
                        You are a job matching assistant. Analyze the CV against the job description.
                        You MUST respond with ONLY a valid JSON object — no markdown, no extra text.

                        {
                          "fitScore": <integer 0-100, must equal the weighted average: sum(score * weight / 100) for each sub-score, rounded to nearest integer>,
                          "recommendation": "<Apply | Apply with changes | Low priority>",
                          "confidence": "<High | Medium | Low>",

                          "subScores": {
                            "skillsMatch":     { "score": <0-100>, "weight": <integer> },
                            "experienceMatch": { "score": <0-100>, "weight": <integer> },
                            "domainMatch":     { "score": <0-100>, "weight": <integer> },
                            "impactMatch":     { "score": <0-100>, "weight": <integer> },
                            "cvPresentation":  { "score": <0-100>, "weight": <integer> }
                          },
                          "weightageReasoning": "<one sentence explaining why you weighted the sub-scores this way for this specific role>",

                          "strengthAlignment": [
                            { "strength": "<specific strength>", "category": "<Skills | Experience Depth | Domain | Seniority | Impact Scale>" }
                          ],

                          "differentiation": ["<what makes this candidate stand out vs typical applicants for this role>"],

                          "gaps": [
                            { "gap": "<specific missing or weak area>", "category": "<Skills | Experience Depth | Domain | Seniority | Impact Scale | Geography | Education>", "severity": "<High | Medium | Low>" }
                          ],

                          "positioningAngle": "<2-3 sentence strategic narrative. Reference the candidate's strongest differentiator, acknowledge the biggest gap and frame around it, and tailor specifically to this company and role.>",

                          "cvAdjustments": [
                            { "adjustment": "<specific CV improvement>", "priority": "<High | Medium | Low>", "addressesGap": "<the specific gap this addresses, or null>" }
                          ]
                        }

                        Sub-score weighting rules:
                        - All 5 weights must sum to exactly 100.
                        - Assign higher weight to dimensions the JD emphasises most.
                        - Domain: weight higher for niche industries (fintech, healthcare, defence, government).
                        - Experience/Seniority: weight higher for senior, staff, or leadership roles.
                        - Skills: weight higher for highly technical or tool-specific roles.
                        - cvPresentation: reflects how well the CV surfaces relevant experience for THIS role — always included.

                        Gaps — evaluate each category and include an entry ONLY if a real gap exists:
                        - Skills: specific tools, frameworks, or certifications required but absent from CV.
                        - Experience Depth: insufficient years or breadth in a required area.
                        - Domain: industry or sector mismatch.
                        - Seniority: candidate is over or under-qualified for the level the JD signals.
                        - Impact Scale: achievements are at a smaller or larger scale than the role expects.
                        - Geography: location requirements or relocation needs mentioned in the JD.
                        - Education: specific degree or qualification listed as required.

                        Confidence:
                        - High: JD is detailed and CV is comprehensive — analysis is reliable.
                        - Medium: JD or CV has some gaps in information.
                        - Low: JD is vague or CV lacks enough detail to assess reliably.

                        Recommendation:
                        - Apply: strong fit, CV presents it well.
                        - Apply with changes: good underlying fit but CV needs targeted improvements before applying.
                        - Low priority: fundamental gaps that are hard to bridge, or very weak fit.
                        """)
                .build();
    }

    public String analyze(String cvText, String jobDescription) {
        return chatClient.prompt()
                .user("CV:\n" + cvText + "\n\nJob Description:\n" + jobDescription)
                .call()
                .content();
    }

    // Used by the scheduler — prepends raw JSearch employer/title as context for LLM normalization
    public String analyze(String cvText, String jobContent, String employerName, String jobTitle) {
        String jobDescription = "Employer: " + employerName + "\nJob Title: " + jobTitle + "\n\n" + jobContent;
        return analyze(cvText, jobDescription);
    }

    public Flux<String> analyzeStream(String cvText, String jobDescription) {
        return chatClient.prompt()
                .user("CV:\n" + cvText + "\n\nJob Description:\n" + jobDescription)
                .stream()
                .content();
    }

    public Flux<String> analyzeStream(String cvText, String jobDescription, String companyName, String roleTitle) {
        String enrichedJob = "Company: " + companyName + "\nRole: " + roleTitle + "\n\n" + jobDescription;
        return analyzeStream(cvText, enrichedJob);
    }
}
