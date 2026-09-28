package com.shivam.jobcopilot.service;

import org.springframework.ai.chat.client.ChatClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;

@Service
public class AIService {

    private static final Logger log = LoggerFactory.getLogger(AIService.class);

    private final ChatClient chatClient;
    private final ChatClient postApplicationChatClient;
    private final ChatClient requirementExtractionChatClient;

    public AIService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder
                .defaultSystem("""
                        You are a job matching assistant. Analyze the CV against the job description.
                        You MUST respond with ONLY a valid JSON object — no markdown, no extra text.
                        The backend calculates the authoritative fit score and recommendation. Do not calculate or return
                        a score, sub-scores, or weights. Your job is to extract JD-supported criteria, assess CV evidence,
                        and provide grounded coaching text.

                        Company identity rules:
                        - companyNameRaw must preserve the company name supplied in the job context.
                        - companyNameCanonical is used for matching applications. Preserve meaningful brand words,
                          including "Company" in names such as "Bain & Company". Remove only clear legal suffixes
                          such as Inc, LLC, Ltd, Limited, Corp, Corporation, GmbH, PLC, Pvt Ltd, or S.A.
                        - Do not invent a different employer. If context is insufficient, use the supplied company
                          name as companyNameCanonical.

                        {
                          "companyNameRaw": "<the company name supplied in the job context>",
                          "companyNameCanonical": "<canonical employer name for matching across applications>",
                          "confidence": "<High | Medium | Low>",

                          "jdRequirements": [
                            {
                              "id": "REQ-1",
                              "requirement": "<plain-English requirement extracted from the JD>",
                              "capability": "<concise capability represented by this requirement>",
                              "importance": "<Core | Supporting | Preferred>",
                              "relevance": "<Direct | Inferred>",
                              "evidenceType": "<ownership | artifact | tool | outcome | domain experience | other>",
                              "sourceExcerpt": "<short excerpt from the JD supporting this requirement>",
                              "evidence": {
                                "status": "<Strong | Good | Weak | Missing>",
                                "evidenceType": "<how the CV demonstrates or fails to demonstrate it>",
                                "evidenceText": "<specific CV-grounded evidence, or null when Missing>",
                                "artifact": "<relevant artifact, or null>",
                                "confidence": "<High | Medium | Low>"
                              }
                            }
                          ],

                          "strengthAlignment": [
                            { "strength": "<specific strength>", "category": "<Skills | Experience Depth | Domain | Seniority | Impact Scale>" }
                          ],

                          "differentiation": ["<what makes this candidate stand out vs typical applicants for this role>"],

                          "gaps": [
                            { "gap": "<specific missing or weak area>", "category": "<Skills | Experience Depth | Domain | Seniority | Impact Scale | Geography | Education>", "severity": "<High | Medium | Low>" }
                          ],

                          "positioningAngle": "<2-3 sentence strategic narrative. Reference the candidate's strongest differentiator, acknowledge the biggest gap and frame around it, and tailor specifically to this company and role.>",

                          "cvAdjustments": [
                            { "adjustment": "<specific CV improvement>", "priority": "<High | Medium | Low>", "addressesGap": "<the specific gap this addresses, or null>", "action": "<rewrite | add | remove>", "cvPoint": "<quote the exact bullet or line from the CV that this adjustment targets — null for 'add' actions>", "suggestedText": "<the concrete rewrite or new bullet text — null for 'remove' actions>" }
                          ]
                        }

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

                        cvAdjustments — rules:
                        - action must be one of: "rewrite" (improve an existing bullet), "add" (new bullet to add), "remove" (delete an existing bullet).
                        - cvPoint: for "rewrite" and "remove" actions, quote the exact bullet or line from the CV verbatim. For "add" actions, set to null.
                        - suggestedText: for "rewrite" and "add" actions, provide the concrete improved or new bullet text ready to paste. For "remove" actions, set to null.
                        - Distribute suggestions across the full CV — do not cluster on opening bullets. Target the most impactful improvements regardless of where they appear in the CV.
                        JD requirement and evidence rules:
                        - jdRequirements is the source of truth for role-grounded evidence. Only create a requirement when it is explicitly stated in the JD or clearly implied by a responsibility described in the JD.
                        - Include only the 5-8 most decision-useful criteria. Consolidate overlapping requirements and responsibilities into one criterion. Do not score the same capability twice.
                        - Core means an explicit minimum qualification or must-have capability, including stated years of experience, required education/certification, or a required skill. A JD statement such as '3-4 years of experience in implementation' is Direct and Core.
                        - Preferred means an explicitly optional, nice-to-have, or preferred qualification. It is still useful, but it must not carry the same weight as a Core requirement.
                        - Supporting means a distinct transferable capability derived from a responsibility. Responsibility-derived criteria must be Supporting and Inferred.
                        - Do not create a criterion from a broad responsibility if it only restates a qualification or describes reporting lines, time windows, oversight, escalation mechanics, pace, accountability, or other operating context.
                        - Do not include location, hybrid attendance, travel, work authorization, or relocation constraints in jdRequirements; those are separate eligibility checks, not capability fit.
                        - Do not create requirements from the role title, general industry assumptions, or skills that appear only in the CV.
                        - relevance must be Direct when the JD names the capability and Inferred when the JD describes a responsibility that clearly requires it.
                        - sourceExcerpt must be grounded in the JD. Do not invent or paraphrase a requirement without JD support.
                        - Evaluate CV evidence only against the requirements in jdRequirements. A CV skill with no matching JD requirement must not appear as requirement evidence.
                        - evidence status Strong means the CV directly and clearly demonstrates the requirement.
                        - Good means the CV demonstrates a substantial transferable version of the capability.
                        - Weak means the CV contains only adjacent or limited evidence.
                        - Missing means the CV does not provide credible evidence.
                        - For Missing evidence, evidenceText and artifact must be null. Never invent candidate experience.
                        - Use stable local ids such as REQ-1, REQ-2 so each evidence object maps to exactly one requirement.
                        - Every evidenceText value must quote a short, exact excerpt from the current CV. If an exact excerpt
                          cannot be found, use Missing rather than paraphrasing or inventing evidence.
                        - Core requirements are explicit must-have qualifications. Preferred requirements remain useful competitive
                          signals even when the JD calls them optional, but Supporting and Preferred criteria must not outweigh Core criteria.
                        - Emit top-level properties in this exact order so the user can receive the analysis progressively:
                          jdRequirements, strengthAlignment, differentiation, gaps, positioningAngle, cvAdjustments.

                        Re-analysis mode:
                        - If the user message contains a Fixed JD rubric, return exactly one jdRequirements entry for every
                          supplied criterion, preserving each supplied id. Put the new assessment in evidence only.
                        - In that mode, do not add, remove, rename, or reweight criteria, and do not use the previous evidence
                          as proof. Reassess the complete updated CV against every supplied criterion.
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

        this.requirementExtractionChatClient = this.chatClient.mutate()
                .defaultSystem("""
                        You extract job-description requirements for downstream career intelligence.
                        Return ONLY a valid JSON object with this shape:
                        {
                          "jdRequirements": [
                            {
                              "id": "REQ-1",
                              "requirement": "<plain-English requirement extracted from the JD>",
                              "capability": "<concise capability represented by this requirement>",
                              "importance": "<Core | Supporting | Preferred>",
                              "relevance": "<Direct | Inferred>",
                              "evidenceType": "<ownership | artifact | tool | outcome | domain experience | other>",
                              "sourceExcerpt": "<short excerpt from the JD supporting this requirement>",
                              "evidence": {
                                "status": "<Strong | Good | Weak | Missing>",
                                "evidenceType": "<how the CV demonstrates or fails to demonstrate it>",
                                "evidenceText": "<specific CV-grounded evidence, or null when Missing>",
                                "artifact": "<relevant artifact, or null when Missing>",
                                "confidence": "<High | Medium | Low>"
                              }
                            }
                          ]
                        }

                        Rules:
                        - Extract only requirements explicitly stated in the JD or clearly implied by a JD responsibility.
                        - Include only the 5-8 most decision-useful criteria. Consolidate overlapping requirements and responsibilities into one criterion. Do not score the same capability twice.
                        - Core means an explicit minimum qualification or must-have capability, including stated years of experience, required education/certification, or a required skill. A JD statement such as '3-4 years of experience in implementation' is Direct and Core.
                        - Preferred means an explicitly optional, nice-to-have, or preferred qualification. It remains useful, but is lower priority than Core.
                        - Supporting means a distinct transferable capability derived from a responsibility. Responsibility-derived criteria must be Supporting and Inferred.
                        - Do not create a criterion from a broad responsibility if it only restates a qualification or describes reporting lines, time windows, oversight, escalation mechanics, pace, accountability, or other operating context.
                        - Ignore reporting lines, time windows, oversight, escalation mechanics, location, hybrid attendance, travel, work authorization, and relocation constraints for capability scoring.
                        - Do not create requirements from the role title, generic industry assumptions, or skills that appear only in the CV.
                        - relevance is Direct when the JD names the capability and Inferred when a responsibility clearly requires it.
                        - sourceExcerpt must be grounded in the JD. Do not invent support for a requirement.
                        - Evaluate CV evidence only against the extracted JD requirements.
                        - Strong means the CV directly and clearly demonstrates the requirement.
                        - Good means the CV demonstrates a substantial transferable version of the capability.
                        - Weak means the CV contains only adjacent or limited evidence.
                        - Missing means there is no credible CV evidence.
                        - For Missing evidence, evidenceText and artifact must be null.
                        - Use stable local ids such as REQ-1, REQ-2 in the order requirements appear.
                        """)
                .build();
    }

    public String analyze(String cvText, String jobDescription) {
        return chatClient.prompt()
                .user("CV:\n" + cvText + "\n\nJob Description:\n" + jobDescription)
                .call()
                .content();
    }

    /** Enriches an existing fit analysis with JD-grounded requirements without recalculating fit. */
    public String extractJdRequirements(String cvText, String jobDescription) {
        return requirementExtractionChatClient.prompt()
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
        return logOpenAiFailures(chatClient.prompt()
                .user("CV:\n" + cvText + "\n\nJob Description:\n" + jobDescription)
                .stream()
                .content());
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

    public Flux<String> reAnalyzeStream(String cvText, String jobDescription,
                                        String companyName, String roleTitle,
                                        String previousContext) {
        String enrichedJob = "Company: " + companyName + "\nRole: " + roleTitle + "\n\n" + jobDescription;
        String userMessage = previousContext + "Updated CV:\n" + cvText + "\n\nJob Description:\n" + enrichedJob;
        return chatClient.prompt()
                .user(userMessage)
                .stream()
                .content();
    }

    private Flux<String> logOpenAiFailures(Flux<String> stream) {
        return stream.doOnError(error -> {
            if (error instanceof WebClientResponseException response) {
                log.error("OpenAI request failed: status={}, body={}", response.getStatusCode(),
                        response.getResponseBodyAsString());
            } else {
                log.error("OpenAI request failed while streaming fit analysis", error);
            }
        });
    }
}
