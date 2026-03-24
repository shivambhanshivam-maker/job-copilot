package com.shivam.jobcopilot.controller;

import com.shivam.jobcopilot.dto.PendingAction;
import com.shivam.jobcopilot.entity.JobApplication;
import com.shivam.jobcopilot.service.JobApplicationService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/job-applications")
public class JobApplicationController {

    private final JobApplicationService jobApplicationService;

    public JobApplicationController(JobApplicationService jobApplicationService) {
        this.jobApplicationService = jobApplicationService;
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
}
