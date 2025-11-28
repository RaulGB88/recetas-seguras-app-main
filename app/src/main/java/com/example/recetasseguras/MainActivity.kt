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
    val authManager = LocalContext.current.let { com.example.recetasseguras.auth.AuthManager.getInstance(it) }

    val snackbarHostState = SnackbarHostState()
    val scope = rememberCoroutineScope()
    val showMessage: (String) -> Unit = { msg ->
        scope.launch { snackbarHostState.showSnackbar(msg) }
    }

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
                AppDestinations.HOME, AppDestinations.FAVORITES -> Greeting(
                    name = "Android",
                    modifier = Modifier.padding(innerPadding)
                )
                AppDestinations.PROFILE -> {
                    if (authManager.getAccessToken() != null) {
                        Greeting(name = "Profile (autenticado)", modifier = Modifier.padding(innerPadding))
                    } else {
                        if (showingRegister) {
                            RegisterScreen(
                                onRegisterSuccess = { showingRegister = false },
                                onGoToLogin = { showingRegister = false },
                                onMessage = showMessage
                            )
                        } else {
                            LoginScreen(
                                onLoginSuccess = { /* refresh UI or navigate */ },
                                onGoToRegister = { showingRegister = true },
                                onMessage = showMessage
                            )
                        }
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
    HOME("Home", Icons.Default.Home),
    FAVORITES("Favorites", Icons.Default.Favorite),
    PROFILE("Profile", Icons.Default.AccountBox),
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
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