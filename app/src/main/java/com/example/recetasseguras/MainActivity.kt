package com.example.recetasseguras

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.runtime.setValue
import com.example.recetasseguras.ui.auth.LoginScreen
import com.example.recetasseguras.ui.auth.RegisterScreen
import com.example.recetasseguras.auth.AuthManager
import com.example.recetasseguras.auth.ConditionDto
import com.example.recetasseguras.auth.RecipeDto
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import com.example.recetasseguras.ui.theme.RecetasSegurasTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import androidx.compose.ui.platform.LocalContext

/**
 * Actividad principal de la aplicación Recetas Seguras.
 * Aquí manejamos el ciclo de vida de la app y configuramos el contenido principal.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RecetasSegurasTheme {
                    RecetasSegurasApp()
            }
        }
    }
}

    @PreviewScreenSizes
    @Composable
    fun RecetasSegurasApp() {
        // Mantenemos el estado de la navegación actual
        var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
        var showingRegister by rememberSaveable { mutableStateOf(false) }
        val context = LocalContext.current
        val authManager = com.example.recetasseguras.auth.AuthManager.getInstance(context)
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        
        // Usamos esta función para mostrar mensajes al usuario
        val showMessage: (String) -> Unit = { msg ->
            scope.launch { snackbarHostState.showSnackbar(msg) }
        }
        
        // ViewModel compartido para toda la app
        val viewModel = remember { com.example.recetasseguras.auth.AuthViewModel(context) }
        val allConditions by viewModel.allConditions.collectAsState()
        val selectedConditions by viewModel.selectedConditions.collectAsState()
        val conditionsLoading by viewModel.conditionsLoading.collectAsState()
        val conditionsError by viewModel.conditionsError.collectAsState()
        val safeFoods by viewModel.safeFoods.collectAsState()
        val safeRecipes by viewModel.safeRecipes.collectAsState()
        val suggestionsLoading by viewModel.suggestionsLoading.collectAsState()
        
        // Estado de autenticación del usuario
        var isAuthenticated by rememberSaveable { mutableStateOf(authManager.getAccessToken() != null) }
        var userId by rememberSaveable { mutableStateOf<Long?>(null) }
        var username by rememberSaveable { mutableStateOf("") }
        var email by rememberSaveable { mutableStateOf("") }
        var needsConditionSelection by rememberSaveable { mutableStateOf(false) }
        var selectedRecipe by remember { mutableStateOf<RecipeDto?>(null) }

        /**
         * Obtenemos los datos del usuario autenticado y cargamos sus condiciones.
         * Si no tiene condiciones seleccionadas, lo llevamos a la pantalla de selección.
         */
        fun fetchUserAndContinue() {
            scope.launch {
                Log.d("MainActivity", "fetchUserAndContinue: Starting")
                val user = viewModel.fetchUser()
                
                // Verificamos si la sesión expiró
                // Si fetchUser devuelve null, puede ser que la sesión expiró
                if (user == null) {
                    Log.d("MainActivity", "fetchUserAndContinue: Failed to fetch user, checking tokens")
                    // Verificar si los tokens fueron limpiados (sesión expirada)
                    if (authManager.getAccessToken() == null) {
                        Log.d("MainActivity", "fetchUserAndContinue: Session expired, redirecting to login")
                        isAuthenticated = false
                        userId = null
                        username = ""
                        email = ""
                        needsConditionSelection = false
                        showMessage("Tu sesión ha expirado. Por favor, inicia sesión nuevamente.")
                        return@launch
                    }
                }
                
                // Guardamos los datos del usuario
                userId = user?.id
                username = user?.username ?: ""
                email = user?.email ?: ""
                isAuthenticated = true
                Log.d("MainActivity", "fetchUserAndContinue: User loaded, userId=$userId")
                
                var userConditions: List<ConditionDto> = emptyList()
                if (userId != null) {
                    // Cargamos todas las condiciones disponibles y las del usuario en paralelo
                    // Cargar todas las condiciones y las del usuario en paralelo
                    Log.d("MainActivity", "fetchUserAndContinue: Loading conditions in parallel")
                    val allConditionsDeferred = async { viewModel.loadAllConditions() }
                    val userConditionsDeferred = async { viewModel.loadUserConditions(userId!!) }
                    
                    // Esperar a que ambas terminen
                    allConditionsDeferred.await()
                    userConditions = userConditionsDeferred.await()
                    Log.d("MainActivity", "fetchUserAndContinue: Loaded ${userConditions.size} conditions")
                } else {
                    viewModel.loadAllConditions()
                }
                
                // Si no tiene condiciones, lo enviamos a seleccionarlas
                needsConditionSelection = userConditions.isEmpty()
                if (needsConditionSelection) {
                    Log.d("MainActivity", "fetchUserAndContinue: No conditions, going to CONDITION_SELECTION")
                    currentDestination = AppDestinations.CONDITION_SELECTION
                } else {
                    Log.d("MainActivity", "fetchUserAndContinue: Has ${userConditions.size} conditions, loading suggestions")
                    currentDestination = AppDestinations.HOME
                    if (userId != null) {
                        viewModel.loadSuggestions(userId!!)
                    }
                }
            }
        }

        // Cargamos los datos del usuario automáticamente si ya está autenticado
        // Cargar datos del usuario automáticamente si hay token guardado
        LaunchedEffect(Unit) {
            Log.d("MainActivity", "LaunchedEffect: isAuthenticated=$isAuthenticated, userId=$userId")
            if (isAuthenticated && userId == null) {
                Log.d("MainActivity", "LaunchedEffect: Calling fetchUserAndContinue")
                fetchUserAndContinue()
            }
        }

        // Monitoreamos si los tokens fueron limpiados mientras la app está abierta
        // Monitorear si los tokens fueron limpiados mientras la app está en uso
        LaunchedEffect(isAuthenticated) {
            if (isAuthenticated) {
                while (true) {
                    kotlinx.coroutines.delay(5000) // Verificamos cada 5 segundos
                    if (authManager.getAccessToken() == null) {
                        Log.d("MainActivity", "Session expired - tokens were cleared")
                        isAuthenticated = false
                        userId = null
                        username = ""
                        email = ""
                        needsConditionSelection = false
                        showMessage("Tu sesión ha expirado. Por favor, inicia sesión nuevamente.")
                        break
                    }
                }
            }
        }

        if (!isAuthenticated) {
            // Mostramos login o registro si no está autenticado
            // Solo mostrar login/registro, bloquear navegación
            if (showingRegister) {
                RegisterScreen(
                    onRegisterSuccess = {
                        fetchUserAndContinue()
                        showingRegister = false
                    },
                    onGoToLogin = { showingRegister = false },
                    onMessage = showMessage
                )
            } else {
                LoginScreen(
                    onLoginSuccess = {
                        fetchUserAndContinue()
                    },
                    onGoToRegister = { showingRegister = true },
                    onMessage = showMessage
                )
            }
        } else {
            // Navegación principal de la app
            // Navegación disponible solo si autenticado
            
            // Si hay una receta seleccionada, mostramos sus detalles
            // Si hay una receta seleccionada, mostrar pantalla de detalles
            if (selectedRecipe != null) {
                com.example.recetasseguras.ui.RecipeDetailScreen(
                    recipe = selectedRecipe!!,
                    onBack = {
                        selectedRecipe = null
                        currentDestination = AppDestinations.HOME
                    }
                )
            } else {
                NavigationSuiteScaffold(
                navigationSuiteItems = {
                    AppDestinations.entries.forEach {
                        item(
                            icon = {
                                Icon(
                                    it.icon,
                                    contentDescription = it.label
                                )
                            },
                            label = { Text(it.label) },
                            selected = it == currentDestination,
                            onClick = { currentDestination = it }
                        )
                    }
                }
            ) {
                Scaffold(modifier = Modifier.fillMaxSize(), snackbarHost = { SnackbarHost(hostState = snackbarHostState) }) { innerPadding ->
                    when (currentDestination) {
                            AppDestinations.HOME -> {
                                com.example.recetasseguras.ui.HomeScreen(
                                    foods = safeFoods,
                                    recipes = safeRecipes,
                                    loading = suggestionsLoading,
                                    error = null,
                                    onRecipeClick = { recipe ->
                                        selectedRecipe = recipe
                                    },
                                    modifier = Modifier.padding(innerPadding)
                                )
                            }
                            AppDestinations.CONDITION_SELECTION -> {
                                com.example.recetasseguras.ui.ConditionSelectionScreen(
                                    allConditions = allConditions,
                                    selectedConditions = selectedConditions,
                                    onAddCondition = { viewModel.addCondition(it) },
                                    onRemoveCondition = { viewModel.removeCondition(it) },
                                    onSave = {
                                        if (userId != null) {
                                            viewModel.saveUserConditions(
                                                userId!!,
                                                onSuccess = {
                                                    needsConditionSelection = false
                                                    viewModel.loadSuggestions(userId!!)
                                                    currentDestination = AppDestinations.HOME
                                                },
                                                onError = { showMessage(it) }
                                            )
                                        }
                                    },
                                    loading = conditionsLoading,
                                    error = conditionsError,
                                    modifier = Modifier.padding(innerPadding)
                                )
                            }
                        AppDestinations.PROFILE -> {
                            com.example.recetasseguras.ui.ProfileScreen(
                                username = username,
                                email = email,
                                viewModel = viewModel,
                                onLogout = {
                                    // Limpiar tokens y estado
                                    authManager.clearTokens()
                                    isAuthenticated = false
                                    userId = null
                                    username = ""
                                    email = ""
                                    needsConditionSelection = false
                                    currentDestination = AppDestinations.HOME
                                },
                                onMessage = showMessage,
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                    }
                }
            }
            }
        }
    }

    /**
     * Destinos de navegación disponibles en la app.
     * Cada uno tiene un label para mostrar y un icono.
     */
    enum class AppDestinations(
        val label: String,
        val icon: ImageVector,
    ) {
        HOME("Sugerencias", Icons.Default.Home),
        CONDITION_SELECTION("Condiciones", Icons.Default.Favorite),
        PROFILE("Perfil", Icons.Default.AccountBox),
    }

    @Composable
    fun Greeting(name: String, modifier: Modifier = Modifier) {
        Text(
            text = "¡Hola, $name!",
            modifier = modifier
        )
    }

    @Preview(showBackground = true)
    @Composable
    fun GreetingPreview() {
        RecetasSegurasTheme {
            Greeting("Android")
        }
    }