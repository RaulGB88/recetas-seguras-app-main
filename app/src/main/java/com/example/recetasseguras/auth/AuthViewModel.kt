package com.example.recetasseguras.auth

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recetasseguras.auth.ConditionDto
import com.example.recetasseguras.auth.UserConditionRequest
import com.example.recetasseguras.auth.FoodDto
import com.example.recetasseguras.auth.RecipeDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.async

/**
 * ViewModel principal para la autenticación y gestión de datos del usuario.
 * Aquí manejamos login, registro, condiciones y sugerencias.
 */
class AuthViewModel(context: Context) : ViewModel() {
        /**
         * Obtenemos los datos del usuario autenticado desde el API.
         * Retorna null si la sesión expiró o hubo un error.
         */
        suspend fun fetchUser(): UserDto? {
            return try {
                val resp = repo.me()
                if (resp.isSuccessful) {
                    resp.body()
                } else {
                    Log.e("AuthViewModel", "fetchUser: Failed with code ${resp.code()}")
                    if (resp.code() == 401) {
                        // Token inválido o expirado - el authenticator ya limpió los tokens
                        Log.e("AuthViewModel", "fetchUser: Unauthorized - session expired")
                    }
                    null
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "fetchUser: Error", e)
                null
            }
        }
    private val repo = AuthRepository.create(context)

    // Estados de carga y errores generales
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // Errores por campo (para validación)
    // Field-level validation messages (field -> message)
    private val _fieldErrors = MutableStateFlow<Map<String, String>>(emptyMap())
    val fieldErrors: StateFlow<Map<String, String>> = _fieldErrors

    // Estados para condiciones
    private val _allConditions = MutableStateFlow<List<ConditionDto>>(emptyList())
    val allConditions: StateFlow<List<ConditionDto>> = _allConditions

    private val _selectedConditions = MutableStateFlow<List<ConditionDto>>(emptyList())
    val selectedConditions: StateFlow<List<ConditionDto>> = _selectedConditions

    private val _conditionsLoading = MutableStateFlow(false)
    val conditionsLoading: StateFlow<Boolean> = _conditionsLoading

    private val _conditionsError = MutableStateFlow<String?>(null)
    val conditionsError: StateFlow<String?> = _conditionsError

    // Estados para sugerencias (comidas y recetas seguras)
    private val _safeFoods = MutableStateFlow<List<FoodDto>?>(null)
    val safeFoods: StateFlow<List<FoodDto>?> = _safeFoods

    private val _safeRecipes = MutableStateFlow<List<RecipeDto>?>(null)
    val safeRecipes: StateFlow<List<RecipeDto>?> = _safeRecipes

    private val _suggestionsLoading = MutableStateFlow(false)
    val suggestionsLoading: StateFlow<Boolean> = _suggestionsLoading

    /**
     * Iniciamos sesión con email y contraseña.
     * Si tiene éxito, llamamos a onSuccess().
     */
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
                        _error.value = apiErr.message?.takeIf { it.isNotBlank() }
                            ?: "No se pudo conectar con el servidor"
                        val map = apiErr.errors
                            ?.mapNotNull { e -> e.field?.let { f -> f to (e.message ?: "") } }
                            ?.toMap() ?: emptyMap()
                        _fieldErrors.value = map
                    } else {
                        _error.value = "No se pudo conectar con el servidor"
                    }
                }
            } catch (t: Throwable) {
                _error.value = "No se pudo conectar con el servidor"
            } finally {
                _loading.value = false
            }
        }
    }

    /**
     * Registramos un nuevo usuario.
     * Si tiene éxito, llamamos a onSuccess().
     */
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
                        _error.value = apiErr.message?.takeIf { it.isNotBlank() }
                            ?: "No se pudo conectar con el servidor"
                        val map = apiErr.errors
                            ?.mapNotNull { e -> e.field?.let { f -> f to (e.message ?: "") } }
                            ?.toMap() ?: emptyMap()
                        _fieldErrors.value = map
                    } else {
                        _error.value = "No se pudo conectar con el servidor"
                    }
                }
            } catch (t: Throwable) {
                _error.value = "No se pudo conectar con el servidor"
            } finally {
                _loading.value = false
            }
        }
    }

    /**
     * Cargamos todas las condiciones disponibles desde el API.
     */
    suspend fun loadAllConditions() {
        _conditionsLoading.value = true
        _conditionsError.value = null
        try {
            val resp = repo.getConditions()
            _allConditions.value = resp
        } catch (e: Exception) {
            _conditionsError.value = "No se pudo cargar las condiciones"
        } finally {
            _conditionsLoading.value = false
        }
    }

    /**
     * Cargamos las condiciones del usuario.
     * Retornamos la lista de condiciones o lista vacía si hay error.
     */
    suspend fun loadUserConditions(userId: Long): List<ConditionDto> {
        Log.d("AuthViewModel", "loadUserConditions: Starting for userId=$userId")
        return try {
            val resp = repo.getUserConditions(userId)
            Log.d("AuthViewModel", "loadUserConditions: Got ${resp.size} conditions")
            _selectedConditions.value = resp
            Log.d("AuthViewModel", "loadUserConditions: Updated _selectedConditions")
            resp
        } catch (e: Exception) {
            Log.e("AuthViewModel", "loadUserConditions: Error loading conditions", e)
            // Si falla, dejar las condiciones vacías para que el usuario las seleccione
            _selectedConditions.value = emptyList()
            emptyList()
        }
    }

    /**
     * Agregamos una condición a la lista de seleccionadas.
     */
    fun addCondition(cond: ConditionDto) {
        _selectedConditions.value = _selectedConditions.value + cond
    }

    /**
     * Quitamos una condición de la lista de seleccionadas.
     */
    fun removeCondition(cond: ConditionDto) {
        _selectedConditions.value = _selectedConditions.value.filter { it.id != cond.id }
    }

    /**
     * Guardamos las condiciones seleccionadas del usuario en el servidor.
     */
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
                    onError("No se pudo guardar las condiciones")
                }
            } catch (e: Exception) {
                onError("No se pudo guardar las condiciones")
            } finally {
                _conditionsLoading.value = false
            }
        }
    }

    /**
     * Cargamos las sugerencias (comidas y recetas seguras) para el usuario.
     * Hacemos ambas llamadas en paralelo para mejorar el rendimiento.
     */
    fun loadSuggestions(userId: Long) {
        viewModelScope.launch {
            _suggestionsLoading.value = true
            Log.d("AuthViewModel", "loadSuggestions: Loading state set to TRUE")
            Log.d("AuthViewModel", "loadSuggestions: Starting for userId=$userId")
            try {
                // Cargar ambas en paralelo
                val foodsDeferred = async { repo.getSafeFoods(userId) }
                val recipesDeferred = async { repo.getSafeRecipes(userId) }
                
                val foods = foodsDeferred.await()
                val recipes = recipesDeferred.await()
                
                Log.d("AuthViewModel", "loadSuggestions: Got ${foods.size} foods and ${recipes.size} recipes")
                _safeFoods.value = foods
                _safeRecipes.value = recipes
            } catch (e: Exception) {
                Log.e("AuthViewModel", "loadSuggestions: Error", e)
            } finally {
                Log.d("AuthViewModel", "loadSuggestions: Loading state set to FALSE")
                _suggestionsLoading.value = false
            }
        }
    }

    /**
     * Cargamos solo las comidas seguras (no usado actualmente).
     */
    fun loadSafeFoods(userId: Long) {
        viewModelScope.launch {
            Log.d("AuthViewModel", "loadSafeFoods: Starting for userId=$userId")
            try {
                val foods = repo.getSafeFoods(userId)
                Log.d("AuthViewModel", "loadSafeFoods: Got ${foods.size} foods")
                _safeFoods.value = foods
            } catch (e: Exception) {
                Log.e("AuthViewModel", "loadSafeFoods: Error", e)
            }
        }
    }

    /**
     * Cargamos solo las recetas seguras (no usado actualmente).
     */
    fun loadSafeRecipes(userId: Long) {
        viewModelScope.launch {
            Log.d("AuthViewModel", "loadSafeRecipes: Starting for userId=$userId")
            try {
                val recipes = repo.getSafeRecipes(userId)
                Log.d("AuthViewModel", "loadSafeRecipes: Got ${recipes.size} recipes")
                _safeRecipes.value = recipes
            } catch (e: Exception) {
                Log.e("AuthViewModel", "loadSafeRecipes: Error", e)
            }
        }
    }

    /**
     * Cerramos la sesión del usuario.
     * Limpiamos los tokens localmente incluso si falla la llamada al API.
     */
    fun logout(onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                repo.logout()
                onSuccess()
            } catch (_: Exception) {
                onSuccess() // Cerramos sesión localmente aunque falle el API
            }
        }
    }

    /**
     * Cambiamos la contraseña del usuario.
     */
    fun changePassword(oldPassword: String, newPassword: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val resp = repo.changePassword(oldPassword, newPassword)
                if (resp.isSuccessful) {
                    onSuccess()
                } else {
                    val body = resp.errorBody()?.string()
                    val apiErr = ApiErrorParser.parse(body)
                    if (apiErr != null) {
                        onError(apiErr.message?.takeIf { it.isNotBlank() } ?: "No se pudo cambiar la contraseña")
                    } else {
                        onError("No se pudo cambiar la contraseña")
                    }
                }
            } catch (t: Throwable) {
                onError("No se pudo conectar con el servidor")
            } finally {
                _loading.value = false
            }
        }
    }
}
