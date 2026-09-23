package com.example.EV_finder_api.repository;

import com.example.EV_finder_api.entity.Issue;
import com.example.EV_finder_api.entity.IssueStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IssueRepository extends JpaRepository<Issue, String> {
    /** A user's own reports — newest first. */
    List<Issue> findByUserIdOrderByCreatedAtDesc(String userId);
    List<Issue> findAllByOrderByCreatedAtDesc();
    long countByStatus(IssueStatus status);
}
