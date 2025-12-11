package com.example.recetasseguras.auth

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface AuthApiService {
    @POST("/api/auth/register")
    suspend fun register(@Body req: RegisterRequest): Response<AuthResponse>

    @POST("/api/auth/login")
    suspend fun login(@Body req: AuthRequest): Response<AuthResponse>

    @POST("/api/auth/refresh")
    suspend fun refresh(@Body req: RefreshRequest): Response<AuthResponse>

    @POST("/api/auth/logout")
    suspend fun logout(@Body req: RefreshRequest): Response<Unit>

    @GET("/api/auth/me")
    suspend fun me(): Response<UserDto>

    @GET("/api/conditions")
    suspend fun getConditions(): List<ConditionDto>

    @GET("/api/users/{id}/conditions")
    suspend fun getUserConditions(@Path("id") userId: Long): List<ConditionDto>

    @POST("/api/users/{id}/conditions")
    suspend fun setUserConditions(@Path("id") userId: Long, @Body body: UserConditionRequest): retrofit2.Response<Unit>

    @GET("/api/users/{id}/safe-foods")
    suspend fun getSafeFoods(@Path("id") userId: Long): List<FoodDto>

    @GET("/api/users/{id}/safe-recipes")
    suspend fun getSafeRecipes(@Path("id") userId: Long): List<RecipeDto>

    @POST("/api/auth/change-password")
    suspend fun changePassword(@Body req: ChangePasswordRequest): Response<Unit>
    
    @GET("/api/admin/stats")
    suspend fun getStats(): retrofit2.Response<Map<String, Long>>
}
