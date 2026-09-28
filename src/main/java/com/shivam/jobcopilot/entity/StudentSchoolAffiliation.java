package com.shivam.jobcopilot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "student_school_affiliations")
public class StudentSchoolAffiliation {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id")
    private Program program;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cohort_id")
    private Cohort cohort;

    private String studentIdentifier;

    @Enumerated(EnumType.STRING)
    private AffiliationStatus status = AffiliationStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    private JobSearchStatus jobSearchStatus = JobSearchStatus.UNKNOWN;

    private LocalDateTime jobSearchStatusUpdatedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public StudentSchoolAffiliation() {}

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public School getSchool() { return school; }
    public void setSchool(School school) { this.school = school; }

    public Program getProgram() { return program; }
    public void setProgram(Program program) { this.program = program; }

    public Cohort getCohort() { return cohort; }
    public void setCohort(Cohort cohort) { this.cohort = cohort; }

    public String getStudentIdentifier() { return studentIdentifier; }
    public void setStudentIdentifier(String studentIdentifier) { this.studentIdentifier = studentIdentifier; }

    public AffiliationStatus getStatus() { return status; }
    public void setStatus(AffiliationStatus status) { this.status = status; }

    public JobSearchStatus getJobSearchStatus() { return jobSearchStatus; }
    public void setJobSearchStatus(JobSearchStatus jobSearchStatus) { this.jobSearchStatus = jobSearchStatus; }

    public LocalDateTime getJobSearchStatusUpdatedAt() { return jobSearchStatusUpdatedAt; }
    public void setJobSearchStatusUpdatedAt(LocalDateTime jobSearchStatusUpdatedAt) { this.jobSearchStatusUpdatedAt = jobSearchStatusUpdatedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
