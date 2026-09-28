package com.shivam.jobcopilot.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record SchoolStudentRosterResponse(
        UUID schoolId,
        String schoolName,
        List<StudentRosterItem> students
) {
    public record StudentRosterItem(
            UUID studentUserId,
            String studentName,
            String studentEmail,
            String programName,
            String cohortName,
            String jobSearchStatus,
            String advisorVisibilityLevel,
            String highestSeverity,
            int supportSignalCount,
            LocalDateTime lastActivityAt,
            SchoolSupportQueueResponse.SupportSummary summary
    ) {}
}
