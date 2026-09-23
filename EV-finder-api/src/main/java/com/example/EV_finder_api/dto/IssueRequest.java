package com.example.EV_finder_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Payload a user submits when reporting a bug or station problem. */
public record IssueRequest(
        @NotBlank @Size(max = 60) String category,
        @NotBlank @Size(max = 150) String subject,
        @NotBlank @Size(max = 2000) String description,
        /** Optional: the station this report is about. */
        String stationId
) {
}
