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

    @GetMapping("/{id}")
    public JobApplication getById(@PathVariable UUID id) {
        return jobApplicationService.getById(id);
    }

    @PostMapping
    public JobApplication create(@RequestBody JobApplication app, Authentication auth) {
        return jobApplicationService.create(app, currentUserId(auth));
    }

    @PutMapping("/{id}")
    public JobApplication update(@PathVariable UUID id, @RequestBody JobApplication app, Authentication auth) {
        return jobApplicationService.update(id, app, currentUserId(auth));
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
