package com.example.recetasseguras.auth

import com.squareup.moshi.Moshi

object ApiErrorParser {
    private val moshi = Moshi.Builder().build()
    private val adapter = moshi.adapter(ApiError::class.java)

    fun parse(json: String?): ApiError? {
        if (json.isNullOrBlank()) return null
        return try {
            adapter.fromJson(json)
        } catch (t: Throwable) {
            null
        }
    }
}
