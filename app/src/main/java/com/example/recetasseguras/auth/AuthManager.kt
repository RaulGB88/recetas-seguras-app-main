package com.example.recetasseguras.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class AuthManager private constructor(private val context: Context) {
    private val prefsName = "auth_prefs"
    private val keyAccess = "access_token"
    private val keyRefresh = "refresh_token"

    private val prefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            prefsName,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveTokens(access: String, refresh: String) {
        prefs.edit().putString(keyAccess, access).putString(keyRefresh, refresh).apply()
    }

    fun getAccessToken(): String? = prefs.getString(keyAccess, null)
    fun getRefreshToken(): String? = prefs.getString(keyRefresh, null)
    fun clearTokens() {
        prefs.edit().remove(keyAccess).remove(keyRefresh).apply()
    }

    companion object {
        @Volatile
        private var instance: AuthManager? = null
        fun getInstance(context: Context): AuthManager =
            instance ?: synchronized(this) {
                instance ?: AuthManager(context.applicationContext).also { instance = it }
            }
    }
}
