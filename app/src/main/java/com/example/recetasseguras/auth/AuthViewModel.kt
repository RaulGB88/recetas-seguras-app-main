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
    // Mensajes de validación por campo (campo -> mensaje)
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

    // Stats (admin)
    private val _stats = MutableStateFlow<Map<String, Long>?>(null)
    val stats: StateFlow<Map<String, Long>?> = _stats

    private val _statsLoading = MutableStateFlow(false)
    val statsLoading: StateFlow<Boolean> = _statsLoading
    
    private val _statsError = MutableStateFlow<String?>(null)
    val statsError: StateFlow<String?> = _statsError

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
                    // Logging temporal para diagnóstico: mostrar código HTTP, body y código parseado
                    try {
                        Log.d("AuthViewModel", "login failed: code=${resp.code()} body=$body parsedCode=${apiErr?.code ?: apiErr?.error}")
                    } catch (t: Throwable) {
                        Log.w("AuthViewModel", "login logging failed: ${t.message}")
                    }
                    if (apiErr != null) {
                        // Mapear errores explícitos por campo si existen
                        val fieldMap = apiErr.errors?.mapNotNull { ve ->
                            val field = ve.field?.trim()
                            val raw = ve.message?.trim()
                            // Intento traducir mensajes de validación comunes antes de mostrarlos
                            val msg = ApiErrorParser.translateValidationMessage(field, raw) ?: raw
                            if (!field.isNullOrBlank() && !msg.isNullOrBlank()) field to msg else null
                        }?.toMap() ?: emptyMap()

                        // Si no hay errores explícitos, inferir algunos códigos comunes como errores por campo
                        val code = apiErr.code ?: apiErr.error
                        val inferred = when (code) {
                            "INVALID_PASSWORD", "PASSWORD_ERROR" -> {
                                val m = ApiErrorParser.userFriendlyMessage(apiErr) ?: apiErr.message
                                if (!m.isNullOrBlank()) mapOf("password" to m) else emptyMap()
                            }
                            "USER_NOT_FOUND" -> {
                                val m = ApiErrorParser.userFriendlyMessage(apiErr) ?: apiErr.message
                                if (!m.isNullOrBlank()) mapOf("email" to m) else emptyMap()
                            }
                            else -> emptyMap()
                        }

                        val merged = fieldMap + inferred

                        if (merged.isNotEmpty()) {
                            _fieldErrors.value = merged
                            // No establecer _error global para evitar duplicados; la UI mostrará los errores en línea
                        } else {
                            _error.value = ApiErrorParser.userFriendlyMessage(apiErr)
                                ?: "No se pudo conectar con el servidor"
                            _fieldErrors.value = emptyMap()
                        }
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
                    try {
                        Log.d("AuthViewModel", "register failed: code=${resp.code()} body=$body parsedCode=${apiErr?.code ?: apiErr?.error}")
                    } catch (t: Throwable) {
                        Log.w("AuthViewModel", "register logging failed: ${t.message}")
                    }
                    if (apiErr != null) {
                        val fieldMap = apiErr.errors?.mapNotNull { ve ->
                            val field = ve.field?.trim()
                            val raw = ve.message?.trim()
                            val msg = ApiErrorParser.translateValidationMessage(field, raw) ?: raw
                            if (!field.isNullOrBlank() && !msg.isNullOrBlank()) field to msg else null
                        }?.toMap() ?: emptyMap()

                        val code = apiErr.code ?: apiErr.error
                        val inferred = when (code) {
                            "PASSWORD_ERROR" -> {
                                val m = ApiErrorParser.userFriendlyMessage(apiErr) ?: apiErr.message
                                if (!m.isNullOrBlank()) mapOf("password" to m) else emptyMap()
                            }
                            else -> emptyMap()
                        }

                        val merged = fieldMap + inferred

                        if (merged.isNotEmpty()) {
                            _fieldErrors.value = merged
                            // Mantener _error en null: preferir errores por campo en línea
                        } else {
                            _error.value = ApiErrorParser.userFriendlyMessage(apiErr)
                                ?: "No se pudo conectar con el servidor"
                            _fieldErrors.value = emptyMap()
                        }
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
     * Cargo todas las condiciones disponibles desde el API.
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
     * Cargo las condiciones del usuario.
     * Retorno la lista de condiciones o lista vacía si hay error.
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
            // Si falla, dejo las condiciones vacías para que el usuario las seleccione
            _selectedConditions.value = emptyList()
            emptyList()
        }
    }

    /**
     * Agrego una condición a la lista de seleccionadas.
     */
    fun addCondition(cond: ConditionDto) {
        _selectedConditions.value = _selectedConditions.value + cond
    }

    /**
     * Quito una condición de la lista de seleccionadas.
     */
    fun removeCondition(cond: ConditionDto) {
        _selectedConditions.value = _selectedConditions.value.filter { it.id != cond.id }
    }

    /**
     * Guardo las condiciones seleccionadas del usuario en el servidor.
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
     * Cargo las sugerencias (comidas y recetas seguras) para el usuario.
     * Hago ambas llamadas en paralelo para mejorar el rendimiento.
     */
    fun loadSuggestions(userId: Long) {
        viewModelScope.launch {
            _suggestionsLoading.value = true
            Log.d("AuthViewModel", "loadSuggestions: Loading state set to TRUE")
            Log.d("AuthViewModel", "loadSuggestions: Starting for userId=$userId")
            try {
                // Cargo ambas en paralelo
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
     * Cargo solo las comidas seguras (no usado actualmente).
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
     * Cargo solo las recetas seguras (no usado actualmente).
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
     * Cargo estadísticas del sistema (solo admin).
     */
    fun loadStats(onError: ((String) -> Unit)? = null) {
        viewModelScope.launch {
            _statsLoading.value = true
            try {
                val resp = repo.getStats()
                if (resp.isSuccessful) {
                    _stats.value = resp.body()
                    _statsError.value = null
                } else {
                    // Intento parsear el body del error para mostrar un mensaje amigable
                    val body = resp.errorBody()?.string()
                    val apiErr = ApiErrorParser.parse(body)
                    val msg = ApiErrorParser.userFriendlyMessage(apiErr)
                        ?: apiErr?.message?.takeIf { it.isNotBlank() }
                        ?: "No se pudieron cargar las estadísticas"
                    Log.e("AuthViewModel", "loadStats: server error code=${resp.code()} body=$body parsed=${apiErr?.code ?: apiErr?.error}")
                    _statsError.value = msg
                    onError?.invoke(msg)
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "loadStats: Error", e)
                _statsError.value = "No se pudieron cargar las estadísticas"
                onError?.invoke("No se pudieron cargar las estadísticas")
            } finally {
                _statsLoading.value = false
            }
        }
    }

    /**
     * Cierro la sesión del usuario.
     * Limpio los tokens localmente incluso si falla la llamada al API.
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
    fun changePassword(oldPassword: String, newPassword: String, confirmPassword: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            _fieldErrors.value = emptyMap()
            try {
                val resp = repo.changePassword(oldPassword, newPassword, confirmPassword)
                    if (resp.isSuccessful) {
                    // Limpiar errores por campo previos al éxito
                    _fieldErrors.value = emptyMap()
                    onSuccess()
                } else {
                    val body = resp.errorBody()?.string()
                    val apiErr = ApiErrorParser.parse(body)
                    try {
                        Log.d("AuthViewModel", "changePassword failed: code=${resp.code()} body=$body parsedCode=${apiErr?.code ?: apiErr?.error}")
                    } catch (t: Throwable) {
                        Log.w("AuthViewModel", "changePassword logging failed: ${t.message}")
                    }
                    if (apiErr != null) {
                        // Mapeo la lista de errores explícitos en fieldErrors
                                val fieldMap = apiErr.errors?.mapNotNull { ve ->
                                    val field = ve.field?.trim()
                                    val raw = ve.message?.trim()
                                    // Intento traducir mensajes de validación por campo; si no hay traducción,
                                    // uso el mensaje crudo o el userFriendlyMessage si está disponible.
                                    val msg = ApiErrorParser.translateValidationMessage(field, raw)
                                        ?: ApiErrorParser.userFriendlyMessage(apiErr)
                                        ?: raw
                            if (!field.isNullOrBlank() && !msg.isNullOrBlank()) field to msg else null
                        }?.toMap() ?: emptyMap()

                        // Si no hay errores explícitos por campo, inferir errores por campo a partir de códigos conocidos
                        val code = apiErr.code ?: apiErr.error
                        val inferred = when (code) {
                            "OLD_PASSWORD_MISMATCH" -> {
                                val m = ApiErrorParser.userFriendlyMessage(apiErr) ?: apiErr.message
                                if (!m.isNullOrBlank()) mapOf("oldPassword" to m) else emptyMap()
                            }
                            "PASSWORD_CONFIRMATION_MISMATCH" -> {
                                val m = ApiErrorParser.userFriendlyMessage(apiErr) ?: apiErr.message
                                if (!m.isNullOrBlank()) mapOf("confirmPassword" to m) else emptyMap()
                            }
                            "PASSWORD_ERROR" -> {
                                // El servidor indica que la nueva contraseña no cumple las reglas (ej. longitud mínima)
                                val m = ApiErrorParser.userFriendlyMessage(apiErr) ?: apiErr.message
                                if (!m.isNullOrBlank()) mapOf("newPassword" to m) else emptyMap()
                            }
                            else -> emptyMap()
                        }

                        val merged = fieldMap + inferred

                        if (merged.isNotEmpty()) {
                            _fieldErrors.value = merged
                            // Si es un código crítico del servidor, también establecer un error global; de lo contrario, mantener solo en línea
                            if (code == "ACCESS_DENIED" || code == "RUNTIME_ERROR" || code == "INTERNAL_ERROR") {
                                _error.value = ApiErrorParser.userFriendlyMessage(apiErr)
                            } else {
                                _error.value = null
                            }
                        } else {
                            // No hay información por campo disponible: usar error global como reserva
                            val msg = ApiErrorParser.userFriendlyMessage(apiErr) ?: "No se pudo cambiar la contraseña"
                            _error.value = msg
                            onError(msg)
                        }
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

    /**
     * Limpia los errores por campo almacenados en el ViewModel.
     * Útil al abrir formularios para evitar mostrar errores previos.
     */
    fun clearFieldErrors() {
        _fieldErrors.value = emptyMap()
    }
}
