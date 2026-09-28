package com.shivam.jobcopilot.repository;

import com.shivam.jobcopilot.entity.School;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SchoolRepository extends JpaRepository<School, UUID> {
    Optional<School> findBySlug(String slug);
    List<School> findByIsActiveTrueOrderByNameAsc();
    boolean existsBySlug(String slug);
}
