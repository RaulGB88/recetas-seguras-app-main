package com.example.recetasseguras.auth

import android.content.Context
import android.util.Log
import com.example.recetasseguras.network.NetworkModule
import retrofit2.Response

class AuthRepository(private val api: AuthApiService, private val authManager: AuthManager) {
    companion object {
        fun create(context: Context): AuthRepository {
            val api = NetworkModule.provideAuthApi(context)
            val mgr = AuthManager.getInstance(context)
            return AuthRepository(api, mgr)
        }
    }

    suspend fun login(email: String, password: String): Response<AuthResponse> {
        Log.d("AuthRepository", "login: email=$email")
        val resp = api.login(AuthRequest(email = email, password = password))
        if (resp.isSuccessful) {
            resp.body()?.let {
                if (it.accessToken != null && it.refreshToken != null) {
                    authManager.saveTokens(it.accessToken, it.refreshToken)
                }
            }
        }
        Log.d("AuthRepository", "login: result=${resp.code()}")
        return resp
    }

    suspend fun register(username: String, email: String, password: String): Response<AuthResponse> {
        Log.d("AuthRepository", "register: username=$username email=$email")
        val resp = api.register(RegisterRequest(username = username, email = email, password = password))
        if (resp.isSuccessful) {
            resp.body()?.let {
                if (it.accessToken != null && it.refreshToken != null) {
                    authManager.saveTokens(it.accessToken, it.refreshToken)
                }
            }
        }
        Log.d("AuthRepository", "register: result=${resp.code()}")
        return resp
    }

    suspend fun logout() {
        authManager.getRefreshToken()?.let {
            api.logout(RefreshRequest(it))
        }
        authManager.clearTokens()
    }

    suspend fun me(): Response<UserDto> = api.me()
}
