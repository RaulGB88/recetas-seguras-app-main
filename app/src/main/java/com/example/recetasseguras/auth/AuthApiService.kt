package com.example.recetasseguras.auth

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

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
}
