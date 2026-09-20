package com.example.EV_finder_api.dto;

import jakarta.validation.constraints.*;

public record ReviewRequest(
        @NotBlank String bookingId,
        @NotNull @Min(1) @Max(5) Integer rating,
        @Size(max = 500) String comment
) {}
