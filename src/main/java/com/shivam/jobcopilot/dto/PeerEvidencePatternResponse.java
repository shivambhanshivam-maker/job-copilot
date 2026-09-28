package com.shivam.jobcopilot.dto;

import java.util.List;
import java.util.UUID;

public record PeerEvidencePatternResponse(
        UUID schoolId,
        String schoolName,
        int peerStudentsIncluded,
        List<Category> categories
) {
    public record Category(
            String roleCategory,
            int interviewApplications,
            int interviewStudents,
            int nonInterviewApplications,
            int nonInterviewStudents,
            List<Pattern> patterns
    ) {}

    public record Pattern(
            String capability,
            int interviewApplicationsWithRequirement,
            int interviewStudentsWithRequirement,
            int interviewStudentsWithEvidence,
            double interviewEvidenceRate,
            int nonInterviewApplicationsWithRequirement,
            int nonInterviewStudentsWithRequirement,
            int nonInterviewStudentsWithEvidence,
            double nonInterviewEvidenceRate,
            int differencePercentagePoints,
            String currentStudentEvidenceStatus,
            int currentStudentApplicationsWithRequirement,
            double currentStudentEvidenceRate
    ) {}
}
