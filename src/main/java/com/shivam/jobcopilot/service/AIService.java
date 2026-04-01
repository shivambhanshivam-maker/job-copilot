package com.shivam.jobcopilot.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class AIService {

    private final ChatClient chatClient;
    private final ChatClient postApplicationChatClient;

    public AIService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder
                .defaultSystem("""
                        You are a job matching assistant. Analyze the CV against the job description.
                        You MUST respond with ONLY a valid JSON object — no markdown, no extra text.

                        {
                          "fitScore": <integer 0-100, must equal the weighted average: sum(score * weight / 100) for each sub-score, rounded to nearest integer>,
                          "recommendation": "<Apply | Optimize & Apply | Ignore — must be derived from fitScore: Apply if ≥80, Optimize & Apply if 60-79, Ignore if <60>",
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

                        Recommendation (must be derived from fitScore — no exceptions):
                        - Apply (fitScore ≥ 80): strong fit. cvAdjustments must be Medium or Low priority only — these are polish, not blockers.
                        - Optimize & Apply (fitScore 60–79): good underlying fit but CV needs targeted improvements. cvAdjustments can include High priority — these are what would lift the score.
                        - Ignore (fitScore < 60): fundamental gaps that CV polish cannot fix. cvAdjustments must be Low priority only.
                        """)
                .build();

        this.postApplicationChatClient = this.chatClient.mutate()
                .defaultSystem("""
                        You are a post-application career coach. The candidate has already submitted their application.
                        Your job is NOT to suggest CV changes — it is too late for that.
                        Instead, help them maximise their chances from this point forward: what to learn, how to position themselves in the interview, what strengths to lead with, and which gaps the interviewer is likely to probe.
                        You MUST respond with ONLY a valid JSON object — no markdown, no extra text.

                        {
                          "fitScore": <integer 0-100, an honest assessment of how well the CV matches the JD>,
                          "interviewAngle": "<2-3 sentences on how the candidate should position themselves in the interview. Lead with their strongest differentiator for this specific role, then acknowledge and reframe their biggest gap.>",
                          "skillGaps": [
                            { "skill": "<specific skill or area they lack>", "priority": "<High | Medium | Low>", "action": "<concrete thing to do now to close this gap before the interview — e.g. build a project, take a short course, read documentation>" }
                          ],
                          "talkingPoints": [
                            "<specific strength from the CV to lead with, framed for this company and role — be concrete, not generic>"
                          ],
                          "redFlags": [
                            { "gap": "<gap the interviewer is likely to probe based on the JD>", "tip": "<how to address it confidently if asked>" }
                          ]
                        }

                        Rules:
                        - skillGaps: only include real gaps — skills or experience clearly required by the JD but absent or weak in the CV.
                        - talkingPoints: 3-5 items, each specific to this company and role. Do not list generic strengths.
                        - redFlags: only include gaps that a hiring manager for this specific role would actually scrutinise. Do not speculate.
                        - fitScore: be honest — a low score here means more prep is needed, not that the application was a mistake.
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

    public Flux<String> analyzePostApplicationStream(String cvText, String jobDescription,
                                                     String companyName, String roleTitle) {
        String enrichedJob = "Company: " + companyName + "\nRole: " + roleTitle + "\n\n" + jobDescription;
        return postApplicationChatClient.prompt()
                .user("CV:\n" + cvText + "\n\nJob Description:\n" + enrichedJob)
                .stream()
                .content();
    }
}
