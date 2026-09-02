package com.example.EV_finder_api.dto;

import jakarta.validation.constraints.*;

public record RegisterRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Email @Size(max = 150) String email,
        @NotBlank @Size(min = 6, max = 100) String password,
        @NotNull Role role
) {
    public enum Role { USER, OPERATOR, ADMIN }
}
