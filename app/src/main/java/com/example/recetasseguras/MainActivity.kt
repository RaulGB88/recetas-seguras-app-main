package com.example.recetasseguras

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext

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
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
    var showingRegister by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val authManager = AuthManager.getInstance(context)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var snackbarJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    val showMessage: (String) -> Unit = { msg ->
        snackbarJob?.cancel()
        snackbarJob = scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(msg)
        }
    }

    val viewModel = remember { com.example.recetasseguras.auth.AuthViewModel(context) }
    val allConditions by viewModel.allConditions.collectAsState()
    val selectedConditions by viewModel.selectedConditions.collectAsState()
    val conditionsLoading by viewModel.conditionsLoading.collectAsState()
    val conditionsError by viewModel.conditionsError.collectAsState()
    val safeFoods by viewModel.safeFoods.collectAsState()
    val safeRecipes by viewModel.safeRecipes.collectAsState()
    val suggestionsLoading by viewModel.suggestionsLoading.collectAsState()

    var isAuthenticated by rememberSaveable { mutableStateOf(authManager.getAccessToken() != null) }
    var userId by rememberSaveable { mutableStateOf<Long?>(null) }
    var username by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var needsConditionSelection by rememberSaveable { mutableStateOf(false) }
    var isAdmin by rememberSaveable { mutableStateOf(false) }
    var userLoaded by rememberSaveable { mutableStateOf(false) }
    var selectedRecipe by remember { mutableStateOf<RecipeDto?>(null) }

    fun fetchUserAndContinue() {
        scope.launch {
            Log.d("MainActivity", "fetchUserAndContinue: Starting")
            val user = viewModel.fetchUser()

            if (user == null) {
                Log.d("MainActivity", "fetchUserAndContinue: Failed to fetch user, checking tokens")
                if (authManager.getAccessToken() == null) {
                    Log.d("MainActivity", "fetchUserAndContinue: Session expired, redirecting to login")
                    isAuthenticated = false
                    userId = null
                    username = ""
                    email = ""
                    needsConditionSelection = false
                    showMessage("Tu sesión ha expirado. Por favor, inicia sesión nuevamente.")
                    userLoaded = true
                    return@launch
                }
            }

            userId = user?.id
            username = user?.username ?: ""
            email = user?.email ?: ""
            isAdmin = user?.role == "ROLE_ADMIN"
            isAuthenticated = true
            Log.d("MainActivity", "fetchUserAndContinue: User loaded, userId=$userId")

            var userConditions: List<ConditionDto> = emptyList()
            if (userId != null) {
                Log.d("MainActivity", "fetchUserAndContinue: Loading conditions in parallel")
                val allConditionsDeferred = async { viewModel.loadAllConditions() }
                val userConditionsDeferred = async { viewModel.loadUserConditions(userId!!) }

                allConditionsDeferred.await()
                userConditions = userConditionsDeferred.await()
                Log.d("MainActivity", "fetchUserAndContinue: Loaded ${'$'}{userConditions.size} conditions")
            } else {
                viewModel.loadAllConditions()
            }

            needsConditionSelection = userConditions.isEmpty()
            if (needsConditionSelection) {
                currentDestination = AppDestinations.CONDITION_SELECTION
            } else {
                currentDestination = AppDestinations.HOME
                if (userId != null) viewModel.loadSuggestions(userId!!)
            }

            userLoaded = true
        }
    }

    LaunchedEffect(Unit) {
        if (isAuthenticated && userId == null) fetchUserAndContinue()
    }

    LaunchedEffect(isAuthenticated) {
        if (isAuthenticated) {
            while (true) {
                kotlinx.coroutines.delay(5000)
                if (authManager.getAccessToken() == null) {
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

    Scaffold(modifier = Modifier.fillMaxSize(), snackbarHost = { SnackbarHost(hostState = snackbarHostState) }) { innerPadding ->
        if (!isAuthenticated) {
            if (showingRegister) {
                RegisterScreen(
                    onRegisterSuccess = {
                        fetchUserAndContinue()
                        showingRegister = false
                    },
                    onGoToLogin = { showingRegister = false },
                    onMessage = showMessage,
                    modifier = Modifier.padding(innerPadding)
                )
            } else {
                LoginScreen(
                    onLoginSuccess = { fetchUserAndContinue() },
                    onGoToRegister = { showingRegister = true },
                    onMessage = showMessage,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        } else {
            if (!userLoaded) {
                Box(modifier = Modifier.padding(innerPadding).fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                if (selectedRecipe != null) {
                    com.example.recetasseguras.ui.RecipeDetailScreen(
                        recipe = selectedRecipe!!,
                        onBack = {
                            selectedRecipe = null
                            currentDestination = AppDestinations.HOME
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                } else {
                    NavigationSuiteScaffold(
                        navigationSuiteItems = {
                            val visible = if (isAdmin) listOf(AppDestinations.SUMMARY, AppDestinations.PROFILE)
                            else listOf(AppDestinations.CONDITION_SELECTION, AppDestinations.HOME, AppDestinations.PROFILE)

                            if (!visible.contains(currentDestination)) {
                                currentDestination = visible.first()
                            }

                            visible.forEach { dest ->
                                item(
                                    icon = { Icon(dest.icon, contentDescription = dest.label) },
                                    label = { Text(dest.label) },
                                    selected = dest == currentDestination,
                                    onClick = { currentDestination = dest }
                                )
                            }
                        }
                    ) {
                        when (currentDestination) {
                            AppDestinations.HOME -> com.example.recetasseguras.ui.HomeScreen(
                                foods = safeFoods,
                                recipes = safeRecipes,
                                loading = suggestionsLoading,
                                error = null,
                                onRecipeClick = { recipe -> selectedRecipe = recipe },
                                modifier = Modifier.padding(innerPadding)
                            )
                            AppDestinations.CONDITION_SELECTION -> com.example.recetasseguras.ui.ConditionSelectionScreen(
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
                            AppDestinations.PROFILE -> com.example.recetasseguras.ui.ProfileScreen(
                                username = username,
                                email = email,
                                viewModel = viewModel,
                                onLogout = {
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
                            AppDestinations.SUMMARY -> com.example.recetasseguras.ui.StatsScreen(
                                viewModel = viewModel,
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

enum class AppDestinations(val label: String, val icon: ImageVector) {
    HOME("Sugerencias", Icons.Default.Home),
    CONDITION_SELECTION("Condiciones", Icons.Default.Favorite),
    PROFILE("Perfil", Icons.Default.AccountBox),
    SUMMARY("Resumen", Icons.Default.Home),
}