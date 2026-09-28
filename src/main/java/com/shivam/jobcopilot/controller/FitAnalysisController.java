package com.shivam.jobcopilot.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shivam.jobcopilot.dto.AdjustmentStateRequest;
import com.shivam.jobcopilot.entity.CV;
import com.shivam.jobcopilot.entity.CvAdjustmentItem;
import com.shivam.jobcopilot.entity.FitAnalysis;
import com.shivam.jobcopilot.entity.GapItem;
import com.shivam.jobcopilot.service.AIService;
import com.shivam.jobcopilot.service.CVService;
import com.shivam.jobcopilot.service.FitAnalysisService;
import com.shivam.jobcopilot.service.FitAnalysisItemStreamParser;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@RestController
public class FitAnalysisController {

    private final CVService cvService;
    private final AIService aiService;
    private final FitAnalysisService fitAnalysisService;
    private final ObjectMapper objectMapper = new ObjectMapper();

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
    public Flux<ServerSentEvent<String>> analyze(@RequestBody Map<String, String> request, Authentication auth) {
        UUID userId = currentUserId(auth);
        UUID cvId = UUID.fromString(request.get("cvId"));
        String jobDescription = request.get("jobDescription");
        String companyName = request.get("companyName");
        String roleTitle = request.get("jobTitle");
        String roleCategory = request.get("roleCategory");

        CV cv = cvService.getById(cvId);

        Optional<FitAnalysis> reusable = fitAnalysisService.findReusableAnalysis(
                userId, cvId, cv.getContentText(), companyName, roleTitle, roleCategory, jobDescription);
        if (reusable.isPresent()) {
            return Flux.just(toSse("[SAVED:" + reusable.get().getId() + "]"));
        }

        StringBuilder buffer = new StringBuilder();

        return streamWithUiEvents(
                aiService.analyzeStream(cv.getContentText(), jobDescription, companyName, roleTitle),
                cv.getContentText(),
                buffer,
                () -> fitAnalysisService.persistFromJson(buffer.toString(), jobDescription, cvId, companyName,
                        roleTitle, roleCategory, userId, cv.getContentText())
        );
    }

    @PostMapping(value = "/fit-analyses/{id}/reanalyze", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> reAnalyze(@PathVariable UUID id,
                                                   @RequestBody(required = false) AdjustmentStateRequest request,
                                                   Authentication auth) {
        FitAnalysis existing = fitAnalysisService.getById(id);
        UUID userId = currentUserId(auth);
        if (existing.getUserId() == null || !userId.equals(existing.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        UUID cvId = existing.getCvId();
        if (request != null && present(request.getCvId())) {
            try {
                cvId = UUID.fromString(request.getCvId().trim());
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid CV id");
            }
        }
        if (cvId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No CV is linked to this analysis");
        }
        CV cv = cvService.getById(cvId);
        if (cv.getUserId() == null || !userId.equals(cv.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        if (cv.getUserId() == null || !userId.equals(cv.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        List<AdjustmentStateRequest.StateUpdate> states = (request != null && request.getStates() != null)
                ? request.getStates()
                : List.of();

        String jobDescription = valueOr(request == null ? null : request.getJobDescription(), existing.getJobDescriptionText());
        String companyName = valueOr(request == null ? null : request.getCompanyName(), existing.getCompany());
        String roleTitle = valueOr(request == null ? null : request.getJobTitle(), existing.getJobTitle());
        String roleCategory = valueOr(request == null ? null : request.getRoleCategory(), existing.getRoleCategory());

        if (!present(jobDescription) || !present(companyName) || !present(roleTitle)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Company, role title, and job description are required");
        }

        // Redaction of dismissed cvPoints from CV text — disabled for now, enable if prompt-based suppression proves unreliable
        // String cvText = cv.getContentText();
        // for (AdjustmentStateRequest.StateUpdate s : states) {
        //     if ("dismissed".equals(s.state()) && s.cvPoint() != null) {
        //         cvText = cvText.replace(s.cvPoint(), "");
        //     }
        // }
        String cvText = cv.getContentText();

        boolean preserveRubric = fitAnalysisService.usesCurrentScoring(existing)
                && existing.getJdRequirements() != null
                && !existing.getJdRequirements().isEmpty()
                && Objects.equals(existing.getJdContentHash(),
                fitAnalysisService.analysisContextHash(companyName, roleTitle, roleCategory, jobDescription));
        String previousContext = preserveRubric ? buildPreviousContext(existing, states) : "";
        StringBuilder buffer = new StringBuilder();

        Flux<String> analysisStream = preserveRubric
                ? aiService.reAnalyzeStream(cvText, jobDescription, companyName, roleTitle, previousContext)
                : aiService.analyzeStream(cvText, jobDescription, companyName, roleTitle);
        final UUID analysisCvId = cvId;

        return streamWithUiEvents(
                analysisStream,
                cvText,
                buffer,
                () -> fitAnalysisService.createRevisionFromJson(
                        buffer.toString(), existing, analysisCvId, cvText, companyName, roleTitle,
                        roleCategory, jobDescription, preserveRubric, userId)
        );
    }

    private Flux<ServerSentEvent<String>> streamWithUiEvents(Flux<String> analysisStream,
                                                             String cvText,
                                                             StringBuilder buffer,
                                                             Supplier<Optional<com.shivam.jobcopilot.entity.FitAnalysis>> persist) {
        FitAnalysisItemStreamParser parser = new FitAnalysisItemStreamParser(objectMapper, fitAnalysisService, cvText);
        AtomicBoolean failed = new AtomicBoolean(false);
        Flux<ServerSentEvent<String>> uiStream = analysisStream
                .doOnNext(buffer::append)
                .flatMapIterable(parser::accept)
                .map(this::serializeEvent)
                .map(this::toSse)
                .doOnError(error -> failed.set(true))
                .onErrorResume(error -> Flux.just(toSse(serializeError(
                        "The fit analysis could not be completed. Please try again."))));
        return Flux.concat(
                Flux.just(toSse(serializeProgress("Reading role requirements"))),
                uiStream,
                Flux.defer(() -> failed.get()
                        ? Flux.empty()
                        : Flux.just(toSse(serializeProgress("Finalizing analysis")))),
                Flux.defer(() -> failed.get() ? Flux.empty() : persist.get()
                        .map(fa -> Flux.just(toSse("[SAVED:" + fa.getId() + "]")))
                        .orElse(Flux.empty()))
        );
    }

    private ServerSentEvent<String> toSse(String data) {
        return ServerSentEvent.<String>builder()
                .data(data)
                .build();
    }

    private String serializeProgress(String stage) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "type", "progress",
                    "stage", stage
            ));
        } catch (Exception e) {
            throw new IllegalStateException("Could not serialize fit-analysis progress", e);
        }
    }

    private String serializeError(String message) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "type", "error",
                    "message", message
            ));
        } catch (Exception e) {
            throw new IllegalStateException("Could not serialize fit-analysis error", e);
        }
    }

    private String serializeEvent(FitAnalysisItemStreamParser.StreamEvent event) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("type", event.type());
            payload.putAll(event.data());
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("Could not serialize fit-analysis stream event", e);
        }
    }

    private String buildPreviousContext(FitAnalysis fa, List<AdjustmentStateRequest.StateUpdate> states) {
        StringBuilder sb = new StringBuilder();
        sb.append("--- Re-Analysis Context ---\n");
        sb.append(fitAnalysisService.fixedRubricContext(fa));
        sb.append("Evaluate the complete updated CV against every fixed criterion. Credit improvements and reflect genuine deterioration. Do not infer evidence from the previous result.\n\n");

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

    private String valueOr(String value, String fallback) {
        return present(value) ? value.trim() : fallback;
    }

    private boolean present(String value) {
        return value != null && !value.isBlank();
    }
}
