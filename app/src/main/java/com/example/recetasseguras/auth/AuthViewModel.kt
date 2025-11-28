package com.example.recetasseguras.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(context: Context) : ViewModel() {
    private val repo = AuthRepository.create(context)
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // Field-level validation messages (field -> message)
    private val _fieldErrors = MutableStateFlow<Map<String, String>>(emptyMap())
    val fieldErrors: StateFlow<Map<String, String>> = _fieldErrors

    fun login(email: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            _fieldErrors.value = emptyMap()
            try {
                val resp = repo.login(email, password)
                if (resp.isSuccessful) {
                    onSuccess()
                } else {
                    val body = resp.errorBody()?.string()
                    val apiErr = ApiErrorParser.parse(body)
                    if (apiErr != null) {
                        _error.value = apiErr.message ?: apiErr.error ?: "Login failed"
                        val map = apiErr.errors
                            ?.mapNotNull { e -> e.field?.let { f -> f to (e.message ?: "") } }
                            ?.toMap() ?: emptyMap()
                        _fieldErrors.value = map
                    } else {
                        _error.value = body ?: "Login failed"
                    }
                }
            } catch (t: Throwable) {
                _error.value = t.localizedMessage ?: "Network error"
            } finally {
                _loading.value = false
            }
        }
    }

    fun register(username: String, email: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            _fieldErrors.value = emptyMap()
            try {
                val resp = repo.register(username, email, password)
                if (resp.isSuccessful) {
                    onSuccess()
                } else {
                    val body = resp.errorBody()?.string()
                    val apiErr = ApiErrorParser.parse(body)
                    if (apiErr != null) {
                        _error.value = apiErr.message ?: apiErr.error ?: "Register failed"
                        val map = apiErr.errors
                            ?.mapNotNull { e -> e.field?.let { f -> f to (e.message ?: "") } }
                            ?.toMap() ?: emptyMap()
                        _fieldErrors.value = map
                    } else {
                        _error.value = body ?: "Register failed"
                    }
                }
            } catch (t: Throwable) {
                _error.value = t.localizedMessage ?: "Network error"
            } finally {
                _loading.value = false
            }
        }
    }
}
