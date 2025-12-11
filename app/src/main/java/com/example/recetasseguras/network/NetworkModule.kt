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

/**
 * Módulo que configura Retrofit, OkHttp y el manejo de tokens JWT.
 * Aquí implementamos el refresh automático de tokens cuando expiran.
 */
object NetworkModule {
    // Aseguro con un lock que solo un refresh se ejecute a la vez (single-flight)
    private val refreshLock = Any()

    /**
     * Configuramos Retrofit con los interceptores necesarios:
     * - Agregamos el token de acceso a cada request
     * - Manejamos el refresh automático cuando un token expira (401)
     * - Logging para desarrollo
     */
    fun provideRetrofit(context: Context, baseUrl: String = BuildConfig.API_BASE_URL): Retrofit {
        val authManager = AuthManager.getInstance(context)

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val client = OkHttpClient.Builder()
            // Interceptor: agrego el token a cada request
            .addInterceptor { chain ->
                val reqBuilder = chain.request().newBuilder()
                authManager.getAccessToken()?.let { token ->
                    reqBuilder.header("Authorization", "Bearer $token")
                }
                val req = reqBuilder.build()
                Log.d("NetworkModule", "request: ${req.method} ${req.url}")
                chain.proceed(req)
            }
            // Authenticator: manejo el refresh cuando recibo 401
            .authenticator { route: Route?, response: Response ->
                // Extraigo el token que falló
                val authHeader = response.request.header("Authorization")
                val failedAccess = authHeader?.removePrefix("Bearer ")

                // No puedo hacer refresh si no hay refresh token disponible
                val refreshToken = authManager.getRefreshToken() ?: return@authenticator null

                // Garantizo single-flight: solo un hilo realiza el refresh; los demás reutilizan el resultado
                synchronized(refreshLock) {
                    // Si otro hilo ya refrescó los tokens, uso el token de acceso más reciente
                    val latestAccess = authManager.getAccessToken()
                    if (!latestAccess.isNullOrEmpty() && latestAccess != failedAccess) {
                        return@synchronized response.request.newBuilder()
                            .header("Authorization", "Bearer $latestAccess")
                            .build()
                    }

                    // Construyo un cliente sincrónico para hacer el refresh
                    // Construyo una API sincrónica para el refresh (Call<T>) y ejecuto de forma bloqueante dentro del authenticator
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
                                // Guardo los nuevos tokens (con rotación del refresh token)
                                authManager.saveTokens(body.accessToken, body.refreshToken)
                                response.request.newBuilder()
                                    .header("Authorization", "Bearer ${body.accessToken}")
                                    .build()
                            } else {
                                authManager.clearTokens()
                                null
                            }
                        } else {
                            // Si falla el refresh, limpio tokens (sesión expirada)
                            authManager.clearTokens()
                            null
                        }
                    } catch (t: Throwable) {
                        // Si ocurre un error durante el refresh, limpio tokens
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

    /**
     * Proveemos una instancia del servicio de autenticación.
     */
    fun provideAuthApi(context: Context, baseUrl: String = BuildConfig.API_BASE_URL): AuthApiService {
        return provideRetrofit(context, baseUrl).create(AuthApiService::class.java)
    }
}
