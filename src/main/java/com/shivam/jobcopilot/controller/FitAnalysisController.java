package com.shivam.jobcopilot.controller;

import com.shivam.jobcopilot.dto.AdjustmentStateRequest;
import com.shivam.jobcopilot.entity.CV;
import com.shivam.jobcopilot.entity.CvAdjustmentItem;
import com.shivam.jobcopilot.entity.FitAnalysis;
import com.shivam.jobcopilot.entity.GapItem;
import com.shivam.jobcopilot.service.AIService;
import com.shivam.jobcopilot.service.CVService;
import com.shivam.jobcopilot.service.FitAnalysisService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
public class FitAnalysisController {

    private final CVService cvService;
    private final AIService aiService;
    private final FitAnalysisService fitAnalysisService;

    public FitAnalysisController(CVService cvService, AIService aiService, FitAnalysisService fitAnalysisService) {
        this.cvService = cvService;
        this.aiService = aiService;
        this.fitAnalysisService = fitAnalysisService;
    }

    private UUID currentUserId(Authentication auth) {
        return (UUID) auth.getPrincipal();
    }

    @GetMapping("/fit-analyses/{id}")
    public FitAnalysis getById(@PathVariable UUID id, Authentication auth) {
        FitAnalysis fa = fitAnalysisService.getById(id);
        if (!currentUserId(auth).equals(fa.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return fa;
    }

    @PostMapping(value = "/match/analyze", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> analyze(@RequestBody Map<String, String> request, Authentication auth) {
        UUID userId = currentUserId(auth);
        UUID cvId = UUID.fromString(request.get("cvId"));
        String jobDescription = request.get("jobDescription");
        String companyName = request.get("companyName");
        String roleTitle = request.get("jobTitle");

        CV cv = cvService.getById(cvId);

        StringBuilder buffer = new StringBuilder();

        return aiService.analyzeStream(cv.getContentText(), jobDescription, companyName, roleTitle)
                .doOnNext(buffer::append)
                .concatWith(Flux.defer(() ->
                        fitAnalysisService.persistFromJson(buffer.toString(), jobDescription, cvId, companyName, roleTitle, userId)
                                .map(fa -> Flux.just("\n[SAVED:" + fa.getId() + "]"))
                                .orElse(Flux.empty())
                ));
    }

    @PostMapping(value = "/fit-analyses/{id}/reanalyze", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> reAnalyze(@PathVariable UUID id,
                                  @RequestBody(required = false) AdjustmentStateRequest request,
                                  Authentication auth) {
        FitAnalysis existing = fitAnalysisService.getById(id);
        CV cv = cvService.getById(existing.getCvId());

        List<AdjustmentStateRequest.StateUpdate> states = (request != null && request.getStates() != null)
                ? request.getStates()
                : List.of();

        // Redaction of dismissed cvPoints from CV text — disabled for now, enable if prompt-based suppression proves unreliable
        // String cvText = cv.getContentText();
        // for (AdjustmentStateRequest.StateUpdate s : states) {
        //     if ("dismissed".equals(s.state()) && s.cvPoint() != null) {
        //         cvText = cvText.replace(s.cvPoint(), "");
        //     }
        // }
        String cvText = cv.getContentText();

        String previousContext = buildPreviousContext(existing, states);
        StringBuilder buffer = new StringBuilder();

        return aiService.reAnalyzeStream(cvText, existing.getJobDescriptionText(),
                        existing.getCompany(), existing.getJobTitle(), previousContext)
                .doOnNext(buffer::append)
                .doOnComplete(() -> fitAnalysisService.replaceFromJson(
                        buffer.toString(), id, existing.getCvId(), currentUserId(auth)
                ));
    }

    private String buildPreviousContext(FitAnalysis fa, List<AdjustmentStateRequest.StateUpdate> states) {
        StringBuilder sb = new StringBuilder();
        sb.append("--- Re-Analysis Context ---\n");
        sb.append("Previous fit score: ").append(fa.getFitScore()).append("\n");
        sb.append("Treat this as a baseline. Do not score lower unless the CV has genuinely worsened. ");
        sb.append("Evaluate the updated CV holistically — credit all improvements whether or not they appear in the previous adjustments list.\n\n");

        if (fa.getGaps() != null && !fa.getGaps().isEmpty()) {
            sb.append("Previous gaps:\n");
            for (GapItem gap : fa.getGaps()) {
                sb.append("- ").append(gap.getGap())
                        .append(" (category: ").append(gap.getCategory())
                        .append(", severity: ").append(gap.getSeverity()).append(")\n");
            }
            sb.append("\n");
        }

        List<AdjustmentStateRequest.StateUpdate> applied   = states.stream().filter(s -> "applied".equals(s.state())).toList();
        List<AdjustmentStateRequest.StateUpdate> dismissed = states.stream().filter(s -> "dismissed".equals(s.state())).toList();

        // Pending = items from previous analysis whose cvPoint was not acted on
        Set<String> actedOnCvPoints = states.stream()
                .filter(s -> s.cvPoint() != null)
                .map(AdjustmentStateRequest.StateUpdate::cvPoint)
                .collect(Collectors.toSet());

        List<CvAdjustmentItem> pending = fa.getCvAdjustments() == null ? List.of() :
                fa.getCvAdjustments().stream()
                        .filter(item -> item.getCvPoint() == null || !actedOnCvPoints.contains(item.getCvPoint()))
                        .toList();

        if (!applied.isEmpty()) {
            sb.append("Applied adjustments — the user has marked these as addressed in their CV.\n");
            sb.append("Compare each against the current CV text below. ");
            sb.append("If fully resolved: do not re-suggest. ");
            sb.append("If only partially addressed: you may suggest further improvement but cap priority at Medium — never High.\n");
            for (AdjustmentStateRequest.StateUpdate s : applied) {
                if (s.cvPoint() != null) sb.append("- original: \"").append(s.cvPoint()).append("\"");
                if (s.suggestedText() != null) sb.append(" → suggested: \"").append(s.suggestedText()).append("\"");
                sb.append("\n");
            }
            sb.append("\n");
        }

        if (!dismissed.isEmpty()) {
            sb.append("The following CV points have been removed from this analysis at the user's request. Do not reference or suggest changes to them:\n");
            for (AdjustmentStateRequest.StateUpdate s : dismissed) {
                if (s.cvPoint() != null) sb.append("- \"").append(s.cvPoint()).append("\"\n");
                else sb.append("- ").append(s.adjustment()).append("\n");
            }
            sb.append("\n");
        }

        if (!pending.isEmpty()) {
            sb.append("Previous adjustments not yet acted on:\n");
            for (CvAdjustmentItem item : pending) {
                sb.append("- [").append(item.getAction()).append("] ");
                if (item.getCvPoint() != null) {
                    sb.append("existing: \"").append(item.getCvPoint()).append("\"");
                    if (item.getSuggestedText() != null) sb.append(" → suggested: \"").append(item.getSuggestedText()).append("\"");
                } else if (item.getSuggestedText() != null) {
                    sb.append("add: \"").append(item.getSuggestedText()).append("\"");
                }
                sb.append("\n");
            }
            sb.append("\n");
        }

        sb.append("---\n\n");
        return sb.toString();
    }
}