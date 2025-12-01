package com.example.recetasseguras

import android.os.Bundle
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import com.example.recetasseguras.ui.theme.RecetasSegurasTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
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
        val authManager = com.example.recetasseguras.auth.AuthManager.getInstance(context)
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        val showMessage: (String) -> Unit = { msg ->
            scope.launch { snackbarHostState.showSnackbar(msg) }
        }
        val viewModel = remember { com.example.recetasseguras.auth.AuthViewModel(context) }
        val allConditions by viewModel.allConditions.collectAsState()
        val selectedConditions by viewModel.selectedConditions.collectAsState()
        val conditionsLoading by viewModel.conditionsLoading.collectAsState()
        val conditionsError by viewModel.conditionsError.collectAsState()
        val safeFoods by viewModel.safeFoods.collectAsState()
        val safeRecipes by viewModel.safeRecipes.collectAsState()
        var userId by rememberSaveable { mutableStateOf<Long?>(null) }
        var username by rememberSaveable { mutableStateOf("") }
        var needsConditionSelection by rememberSaveable { mutableStateOf(false) }

        fun fetchUserAndContinue() {
            scope.launch {
                val user = viewModel.fetchUser()
                userId = user?.id
                username = user?.username ?: ""
                viewModel.loadAllConditions()
                needsConditionSelection = selectedConditions.isEmpty()
                if (needsConditionSelection) {
                    currentDestination = AppDestinations.CONDITION_SELECTION
                } else {
                    if (userId != null) {
                        viewModel.loadSafeFoods(userId!!)
                        viewModel.loadSafeRecipes(userId!!)
                    }
                    currentDestination = AppDestinations.HOME
                }
            }
        }

        if (authManager.getAccessToken() == null || userId == null) {
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
            // Navegación disponible solo si autenticado
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
                                    loading = false,
                                    error = null,
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
                                                    viewModel.loadSafeFoods(userId!!)
                                                    viewModel.loadSafeRecipes(userId!!)
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
                            Greeting(name = "Perfil: $username", modifier = Modifier.padding(innerPadding))
                        }
                    }
                }
            }
        }
    }

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