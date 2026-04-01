package com.shivam.jobcopilot.repository;

import com.shivam.jobcopilot.entity.PostApplicationInsight;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PostApplicationInsightRepository extends JpaRepository<PostApplicationInsight, UUID> {

    Optional<PostApplicationInsight> findByJobApplicationId(UUID jobApplicationId);

    List<PostApplicationInsight> findAllByJobApplicationIdIn(Collection<UUID> jobApplicationIds);
}
