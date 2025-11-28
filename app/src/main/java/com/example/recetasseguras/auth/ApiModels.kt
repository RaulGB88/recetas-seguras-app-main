package com.example.recetasseguras.auth

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RegisterRequest(val username: String, val email: String, val password: String)

@JsonClass(generateAdapter = true)
data class AuthRequest(val email: String, val password: String)

@JsonClass(generateAdapter = true)
data class RefreshRequest(val refreshToken: String)

@JsonClass(generateAdapter = true)
data class AuthResponse(
    val accessToken: String?,
    val refreshToken: String?,
    val tokenType: String? = "Bearer",
    val expiresIn: Long? = null,
    val userId: Long? = null
)

@JsonClass(generateAdapter = true)
data class UserDto(val id: Long?, val username: String?, val email: String?)

@JsonClass(generateAdapter = true)
data class ValidationError(val field: String?, val message: String?)

@JsonClass(generateAdapter = true)
data class ApiError(
    val timestamp: String?,
    val status: Int?,
    val error: String?,
    val message: String?,
    val errors: List<ValidationError>?
)
