package com.shivam.jobcopilot.repository;

import com.shivam.jobcopilot.entity.Program;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProgramRepository extends JpaRepository<Program, UUID> {
    List<Program> findBySchoolIdAndIsActiveTrueOrderByNameAsc(UUID schoolId);
}
