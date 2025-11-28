package com.example.recetasseguras.network

import android.content.Context
import com.example.recetasseguras.BuildConfig
import com.example.recetasseguras.auth.AuthApiService
import com.example.recetasseguras.auth.AuthManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import android.util.Log
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {
    // Single lock used to ensure only one refresh runs at a time (single-flight)
    private val refreshLock = Any()

    fun provideRetrofit(context: Context, baseUrl: String = BuildConfig.API_BASE_URL): Retrofit {
        val authManager = AuthManager.getInstance(context)

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val reqBuilder = chain.request().newBuilder()
                authManager.getAccessToken()?.let { token ->
                    reqBuilder.header("Authorization", "Bearer $token")
                }
                val req = reqBuilder.build()
                Log.d("NetworkModule", "request: ${req.method} ${req.url}")
                chain.proceed(req)
            }
            .authenticator { route: Route?, response: Response ->
                // Extract the access token used in the failed request
                val authHeader = response.request.header("Authorization")
                val failedAccess = authHeader?.removePrefix("Bearer ")

                // Quick exit if no refresh token available
                val refreshToken = authManager.getRefreshToken() ?: return@authenticator null

                // Single-flight: only one thread will perform refresh; others will reuse result
                synchronized(refreshLock) {
                    // If another thread already refreshed tokens, use the latest access token
                    val latestAccess = authManager.getAccessToken()
                    if (!latestAccess.isNullOrEmpty() && latestAccess != failedAccess) {
                        return@synchronized response.request.newBuilder()
                            .header("Authorization", "Bearer $latestAccess")
                            .build()
                    }

                    // Build a synchronous API for refresh (Call<T>) to execute blocking inside authenticator
                    val moshi = Moshi.Builder()
                        .add(KotlinJsonAdapterFactory())
                        .build()
                    val retrofit = Retrofit.Builder()
                        .baseUrl(baseUrl)
                        .addConverterFactory(MoshiConverterFactory.create(moshi))
                        .client(OkHttpClient.Builder().build())
                        .build()
                    val apiSync = retrofit.create(com.example.recetasseguras.auth.AuthApiServiceSync::class.java)

                    return@synchronized try {
                        val call = apiSync.refresh(com.example.recetasseguras.auth.RefreshRequest(refreshToken))
                        val refreshResp = call.execute()
                        if (refreshResp.isSuccessful) {
                            val body = refreshResp.body()
                            if (body?.accessToken != null && body.refreshToken != null) {
                                authManager.saveTokens(body.accessToken, body.refreshToken)
                                response.request.newBuilder()
                                    .header("Authorization", "Bearer ${body.accessToken}")
                                    .build()
                            } else {
                                authManager.clearTokens()
                                null
                            }
                        } else {
                            authManager.clearTokens()
                            null
                        }
                    } catch (t: Throwable) {
                        authManager.clearTokens()
                        null
                    }
                }
            }
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    fun provideAuthApi(context: Context, baseUrl: String = BuildConfig.API_BASE_URL): AuthApiService {
        return provideRetrofit(context, baseUrl).create(AuthApiService::class.java)
    }
}
