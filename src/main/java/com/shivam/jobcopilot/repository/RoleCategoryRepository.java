package com.shivam.jobcopilot.repository;

import com.shivam.jobcopilot.entity.RoleCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RoleCategoryRepository extends JpaRepository<RoleCategory, UUID> {
    boolean existsByName(String name);
}