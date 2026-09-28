package com.shivam.jobcopilot.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "capability_concepts")
public class CapabilityConcept {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "capability_concept_aliases", joinColumns = @JoinColumn(name = "concept_id"))
    @Column(name = "alias", columnDefinition = "TEXT")
    private Set<String> aliases = new LinkedHashSet<>();

    private String status = "CANDIDATE";

    private LocalDateTime createdAt;

    public CapabilityConcept() {}

    public CapabilityConcept(String name, String description, Set<String> aliases) {
        this.name = name;
        this.description = description;
        this.aliases = aliases;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Set<String> getAliases() { return aliases; }
    public void setAliases(Set<String> aliases) { this.aliases = aliases; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
