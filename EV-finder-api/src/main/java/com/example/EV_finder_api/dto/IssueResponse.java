package com.example.EV_finder_api.dto;

import com.example.EV_finder_api.entity.Issue;
import com.example.EV_finder_api.entity.IssueStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record IssueResponse(
        String id,
        String userId,
        String userName,
        String userEmail,
        String stationId,
        String stationName,
        String category,
        String subject,
        String description,
        IssueStatus status,
        String resolutionNote,
        LocalDateTime createdAt,
        LocalDateTime resolvedAt
) {
    public static IssueResponse from(Issue i) {
        return new IssueResponse(
                i.getId(),
                i.getUser().getId(), i.getUser().getName(), i.getUser().getEmail(),
                i.getStation() != null ? i.getStation().getId() : null,
                i.getStation() != null ? i.getStation().getName() : null,
                i.getCategory(), i.getSubject(), i.getDescription(),
                i.getStatus(), i.getResolutionNote(), i.getCreatedAt(), i.getResolvedAt());
    }
}
