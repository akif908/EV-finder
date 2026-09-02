package com.example.evfinder.core.model

/** Mirrors the backend RegisterRequest DTO (Phase 2). */
data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val role: String // USER | OPERATOR | ADMIN
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class AuthResponse(
    val token: String,
    val userId: String,
    val name: String,
    val email: String,
    val role: String
)
