package com.shivam.jobcopilot.repository;

import com.shivam.jobcopilot.entity.CapabilityConcept;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CapabilityConceptRepository extends JpaRepository<CapabilityConcept, UUID> {
    Optional<CapabilityConcept> findByNameIgnoreCase(String name);
}
