package com.example.recetasseguras.auth

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RegisterRequest(val username: String, val email: String, val password: String)

@JsonClass(generateAdapter = true)
data class AuthRequest(val email: String, val password: String)

@JsonClass(generateAdapter = true)
data class ChangePasswordRequest(val oldPassword: String, val newPassword: String, val confirmPassword: String)

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
data class UserDto(val id: Long?, val username: String?, val email: String?, val role: String? = null)

@JsonClass(generateAdapter = true)
data class ValidationError(val field: String?, val message: String?)

@JsonClass(generateAdapter = true)
data class ApiError(
    val timestamp: String?,
    val status: Int?,
    val error: String?,
    val code: String?,
    val message: String?,
    val errors: List<ValidationError>?
)

@JsonClass(generateAdapter = true)
data class ConditionDto(val id: Int, val name: String, val conditionType: String)

@JsonClass(generateAdapter = true)
data class UserConditionRequest(val conditionIds: List<Int>)

@JsonClass(generateAdapter = true)
data class FoodDto(val id: Int, val name: String, val category: String)

@JsonClass(generateAdapter = true)
data class RecipeIngredientDto(
    val foodId: Int? = null,
    val foodName: String? = null,
    val quantity: String? = null
)

@JsonClass(generateAdapter = true)
data class RecipeDto(
    val id: Int? = null,
    val title: String? = null,
    val description: String? = null,
    val steps: String? = null,
    val ingredients: List<RecipeIngredientDto>? = null
)
