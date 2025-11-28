package com.example.recetasseguras.auth

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthApiServiceSync {
    @POST("/api/auth/register")
    fun register(@Body req: RegisterRequest): Call<AuthResponse>

    @POST("/api/auth/login")
    fun login(@Body req: AuthRequest): Call<AuthResponse>

    @POST("/api/auth/refresh")
    fun refresh(@Body req: RefreshRequest): Call<AuthResponse>

    @POST("/api/auth/logout")
    fun logout(@Body req: RefreshRequest): Call<Void>

    @GET("/api/auth/me")
    fun me(): Call<UserDto>
}
