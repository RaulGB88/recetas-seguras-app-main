package com.example.recetasseguras.auth

import android.content.Context
import android.util.Log
import com.example.recetasseguras.network.NetworkModule
import retrofit2.Response

class AuthRepository(private val api: AuthApiService, private val authManager: AuthManager) {
        suspend fun getConditions(): List<ConditionDto> {
            return api.getConditions()
        }

        suspend fun getUserConditions(userId: Long): List<ConditionDto> {
            return api.getUserConditions(userId)
        }

        suspend fun setUserConditions(userId: Long, body: UserConditionRequest): retrofit2.Response<Unit> {
            return api.setUserConditions(userId, body)
        }

        suspend fun getSafeFoods(userId: Long): List<FoodDto> {
            return api.getSafeFoods(userId)
        }

        suspend fun getSafeRecipes(userId: Long): List<RecipeDto> {
            return api.getSafeRecipes(userId)
        }
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

    suspend fun changePassword(oldPassword: String, newPassword: String, confirmPassword: String): Response<Unit> {
        return api.changePassword(ChangePasswordRequest(oldPassword, newPassword, confirmPassword))
    }

    suspend fun getStats(): retrofit2.Response<Map<String, Long>> {
        return api.getStats()
    }

    suspend fun me(): Response<UserDto> = api.me()
}
