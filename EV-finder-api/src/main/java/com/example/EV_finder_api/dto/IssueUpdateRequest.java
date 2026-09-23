package com.example.EV_finder_api.dto;

import jakarta.validation.constraints.Size;

/** Admin's response to a report: new status plus an optional note for the user. */
public record IssueUpdateRequest(
        String status,
        @Size(max = 2000) String resolutionNote
) {
}
