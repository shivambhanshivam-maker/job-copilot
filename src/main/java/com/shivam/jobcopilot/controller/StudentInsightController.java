package com.shivam.jobcopilot.controller;

import com.shivam.jobcopilot.dto.StudentInsightResponse;
import com.shivam.jobcopilot.entity.StudentInsight;
import com.shivam.jobcopilot.service.StudentInsightService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class StudentInsightController {

    private final StudentInsightService studentInsightService;

    public StudentInsightController(StudentInsightService studentInsightService) {
        this.studentInsightService = studentInsightService;
    }

    private UUID currentUserId(Authentication auth) {
        return (UUID) auth.getPrincipal();
    }

    @GetMapping("/student/insights")
    public StudentInsightResponse getStudentInsights(Authentication auth) {
        // Insights are regenerated asynchronously when applications or fit analyses change.
        // Page loads should only read the latest stored result.
        List<StudentInsight> insights = studentInsightService.listActive(currentUserId(auth));
        return new StudentInsightResponse(
                studentInsightService.selectPrimaryFocus(insights)
                        .map(StudentInsightResponse.PrimaryFocus::from)
                        .orElse(null),
                insights.stream()
                        .map(StudentInsightResponse.Item::from)
                        .toList()
        );
    }
}
