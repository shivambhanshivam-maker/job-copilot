package com.shivam.jobcopilot.controller;

import com.shivam.jobcopilot.dto.PendingAction;
import com.shivam.jobcopilot.dto.PostApplicationInsightResult;
import com.shivam.jobcopilot.entity.CV;
import com.shivam.jobcopilot.entity.JobApplication;
import com.shivam.jobcopilot.entity.PostApplicationInsight;
import com.shivam.jobcopilot.service.AIService;
import com.shivam.jobcopilot.service.CVService;
import com.shivam.jobcopilot.service.JobApplicationService;
import com.shivam.jobcopilot.service.PostApplicationInsightService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/job-applications")
public class JobApplicationController {

    private final JobApplicationService jobApplicationService;
    private final PostApplicationInsightService insightService;
    private final CVService cvService;
    private final AIService aiService;

    public JobApplicationController(JobApplicationService jobApplicationService,
                                    PostApplicationInsightService insightService,
                                    CVService cvService,
                                    AIService aiService) {
        this.jobApplicationService = jobApplicationService;
        this.insightService = insightService;
        this.cvService = cvService;
        this.aiService = aiService;
    }

    private UUID currentUserId(Authentication auth) {
        return (UUID) auth.getPrincipal();
    }

    @GetMapping("/pending-actions")
    public List<PendingAction> getPendingActions(Authentication auth) {
        return jobApplicationService.getPendingActions(currentUserId(auth));
    }

    @GetMapping
    public List<JobApplication> listAll(Authentication auth) {
        return jobApplicationService.listAll(currentUserId(auth));
    }

    @GetMapping("/email-reviews")
    public List<com.shivam.jobcopilot.dto.EmailReviewResponse> getEmailReviews(Authentication auth) {
        return jobApplicationService.getEmailReviews(currentUserId(auth));
    }

    public record EmailReviewResolutionRequest(UUID applicationId) {}

    @PostMapping("/email-reviews/{reviewId}/resolve")
    public JobApplication resolveEmailReview(@PathVariable UUID reviewId,
                                             @RequestBody EmailReviewResolutionRequest request,
                                             Authentication auth) {
        if (request == null || request.applicationId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "An application must be selected");
        }
        try {
            return jobApplicationService.resolveEmailReview(reviewId, request.applicationId(), currentUserId(auth));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (SecurityException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
        }
    }

    @PostMapping("/email-reviews/{reviewId}/ignore")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void ignoreEmailReview(@PathVariable UUID reviewId, Authentication auth) {
        try {
            jobApplicationService.ignoreEmailReview(reviewId, currentUserId(auth));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (SecurityException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public JobApplication getById(@PathVariable UUID id) {
        return jobApplicationService.getById(id);
    }

    @PostMapping
    public JobApplication create(@RequestBody JobApplication app, Authentication auth) {
        try {
            return jobApplicationService.create(app, currentUserId(auth));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping("/{id}/fit-analysis/retry")
    public JobApplication retryFitAnalysis(@PathVariable UUID id, Authentication auth) {
        try {
            return jobApplicationService.retryFitAnalysis(id, currentUserId(auth));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (SecurityException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
        }
    }

    @PostMapping("/from-fit-analysis/{fitAnalysisId}")
    public JobApplication markAsApplied(@PathVariable UUID fitAnalysisId,
                                        @RequestBody(required = false) Map<String, String> request,
                                        Authentication auth) {
        try {
            String roleCategory = request == null ? null : request.get("roleCategory");
            return jobApplicationService.markAsApplied(fitAnalysisId, currentUserId(auth), roleCategory);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (SecurityException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public JobApplication update(@PathVariable UUID id, @RequestBody JobApplication app, Authentication auth) {
        try {
            return jobApplicationService.update(id, app, currentUserId(auth));
        } catch (SecurityException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
        }
    }

    @PostMapping("/{id}/snooze")
    public JobApplication snooze(@PathVariable UUID id,
                                 @RequestParam(defaultValue = "0") int days) {
        return jobApplicationService.snooze(id, days);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        jobApplicationService.delete(id);
    }

    // --- Post-application insight ---

    @GetMapping("/{id}/post-match")
    public PostApplicationInsightResult getInsight(@PathVariable UUID id) {
        JobApplication app = jobApplicationService.getById(id);
        PostApplicationInsight insight = insightService.findByApplicationId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No insight found for this application"));
        boolean stale = insightService.isStale(insight, app.getCvId(), app.getJobDescriptionText());
        return new PostApplicationInsightResult(insight, stale);
    }

    @PostMapping(value = "/{id}/post-match", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> analyze(@PathVariable UUID id, Authentication auth) {
        UUID userId = currentUserId(auth);
        JobApplication app = jobApplicationService.getById(id);

        if (app.getCvId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No CV linked to this application");
        }
        if (app.getJobDescriptionText() == null || app.getJobDescriptionText().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No job description on this application");
        }

        CV cv = cvService.getById(app.getCvId());
        StringBuilder buffer = new StringBuilder();

        return aiService.analyzePostApplicationStream(
                        cv.getContentText(),
                        app.getJobDescriptionText(),
                        app.getCompany(),
                        app.getJobTitle())
                .doOnNext(buffer::append)
                .doOnComplete(() -> insightService.persistFromJson(
                        buffer.toString(), id, app.getCvId(), app.getJobDescriptionText(), userId
                ));
    }
}
