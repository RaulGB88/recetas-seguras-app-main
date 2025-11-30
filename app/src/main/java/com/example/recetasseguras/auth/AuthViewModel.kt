package com.example.recetasseguras.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recetasseguras.auth.ConditionDto
import com.example.recetasseguras.auth.UserConditionRequest
import com.example.recetasseguras.auth.FoodDto
import com.example.recetasseguras.auth.RecipeDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(context: Context) : ViewModel() {
        suspend fun fetchUser(): UserDto? {
            val resp = repo.me()
            return if (resp.isSuccessful) resp.body() else null
        }
    private val repo = AuthRepository.create(context)

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // Field-level validation messages (field -> message)
    private val _fieldErrors = MutableStateFlow<Map<String, String>>(emptyMap())
    val fieldErrors: StateFlow<Map<String, String>> = _fieldErrors

    private val _allConditions = MutableStateFlow<List<ConditionDto>>(emptyList())
    val allConditions: StateFlow<List<ConditionDto>> = _allConditions

    private val _selectedConditions = MutableStateFlow<List<ConditionDto>>(emptyList())
    val selectedConditions: StateFlow<List<ConditionDto>> = _selectedConditions

    private val _conditionsLoading = MutableStateFlow(false)
    val conditionsLoading: StateFlow<Boolean> = _conditionsLoading

    private val _conditionsError = MutableStateFlow<String?>(null)
    val conditionsError: StateFlow<String?> = _conditionsError

    private val _safeFoods = MutableStateFlow<List<FoodDto>>(emptyList())
    val safeFoods: StateFlow<List<FoodDto>> = _safeFoods

    private val _safeRecipes = MutableStateFlow<List<RecipeDto>>(emptyList())
    val safeRecipes: StateFlow<List<RecipeDto>> = _safeRecipes

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

    fun loadAllConditions() {
        viewModelScope.launch {
            _conditionsLoading.value = true
            _conditionsError.value = null
            try {
                val result = repo.getConditions()
                _allConditions.value = result
            } catch (e: Exception) {
                _conditionsError.value = e.localizedMessage ?: "Error al cargar condiciones"
            } finally {
                _conditionsLoading.value = false
            }
        }
    }

    fun addCondition(cond: ConditionDto) {
        _selectedConditions.value = _selectedConditions.value + cond
    }

    fun removeCondition(cond: ConditionDto) {
        _selectedConditions.value = _selectedConditions.value.filter { it.id != cond.id }
    }

    fun saveUserConditions(userId: Long, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _conditionsLoading.value = true
            _conditionsError.value = null
            try {
                val ids = _selectedConditions.value.map { it.id }
                val resp = repo.setUserConditions(userId, UserConditionRequest(ids))
                if (resp.isSuccessful) {
                    onSuccess()
                } else {
                    onError("Error al guardar condiciones")
                }
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Error al guardar condiciones")
            } finally {
                _conditionsLoading.value = false
            }
        }
    }

    fun loadSafeFoods(userId: Long) {
        viewModelScope.launch {
            try {
                val foods = repo.getSafeFoods(userId)
                _safeFoods.value = foods
            } catch (_: Exception) {}
        }
    }

    fun loadSafeRecipes(userId: Long) {
        viewModelScope.launch {
            try {
                val recipes = repo.getSafeRecipes(userId)
                _safeRecipes.value = recipes
            } catch (_: Exception) {}
        }
    }
}
