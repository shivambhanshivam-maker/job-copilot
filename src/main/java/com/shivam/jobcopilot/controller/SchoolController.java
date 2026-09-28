package com.shivam.jobcopilot.controller;

import com.shivam.jobcopilot.dto.SchoolContextResponse;
import com.shivam.jobcopilot.dto.SchoolOverviewResponse;
import com.shivam.jobcopilot.dto.SchoolSupportQueueActionRequest;
import com.shivam.jobcopilot.dto.SchoolStudentApplicationsResponse;
import com.shivam.jobcopilot.dto.SchoolStudentRosterResponse;
import com.shivam.jobcopilot.dto.SchoolSupportQueueResponse;
import com.shivam.jobcopilot.service.SchoolAnalyticsService;
import com.shivam.jobcopilot.service.SchoolContextService;
import com.shivam.jobcopilot.service.SchoolSupportQueueService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/school")
public class SchoolController {

    private final SchoolContextService schoolContextService;
    private final SchoolAnalyticsService schoolAnalyticsService;
    private final SchoolSupportQueueService schoolSupportQueueService;

    public SchoolController(SchoolContextService schoolContextService,
                            SchoolAnalyticsService schoolAnalyticsService,
                            SchoolSupportQueueService schoolSupportQueueService) {
        this.schoolContextService = schoolContextService;
        this.schoolAnalyticsService = schoolAnalyticsService;
        this.schoolSupportQueueService = schoolSupportQueueService;
    }

    private UUID currentUserId(Authentication auth) {
        return (UUID) auth.getPrincipal();
    }

    @GetMapping("/context")
    public SchoolContextResponse getContext(Authentication auth) {
        return schoolContextService.getContext(currentUserId(auth));
    }

    @GetMapping("/overview")
    public SchoolOverviewResponse getOverview(Authentication auth) {
        return schoolAnalyticsService.getOverview(currentUserId(auth));
    }

    @GetMapping("/support-queue")
    public SchoolSupportQueueResponse getSupportQueue(Authentication auth) {
        return schoolSupportQueueService.getSupportQueue(currentUserId(auth));
    }

    @PostMapping("/support-queue/review")
    public void reviewSupportSignal(@RequestBody SchoolSupportQueueActionRequest request,
                                    Authentication auth) {
        schoolSupportQueueService.markSupportSignalReviewed(currentUserId(auth), request);
    }

    @PostMapping("/support-queue/snooze")
    public void snoozeSupportSignal(@RequestBody SchoolSupportQueueActionRequest request,
                                    Authentication auth) {
        schoolSupportQueueService.snoozeSupportSignal(currentUserId(auth), request);
    }

    @GetMapping("/students")
    public SchoolStudentRosterResponse getStudents(Authentication auth) {
        return schoolSupportQueueService.getStudentRoster(currentUserId(auth));
    }

    @GetMapping("/students/{studentUserId}/applications")
    public SchoolStudentApplicationsResponse getStudentApplications(@PathVariable UUID studentUserId,
                                                                    Authentication auth) {
        return schoolSupportQueueService.getStudentApplications(currentUserId(auth), studentUserId);
    }
}
