package com.example.recetasseguras.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.recetasseguras.auth.AuthViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onGoToRegister: () -> Unit,
    onMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val vm: AuthViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(context) as T
        }
    })

    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()
    val fieldErrors by vm.fieldErrors.collectAsState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Show snackbar for error messages via the provided callback (copy to local var to avoid smart-cast issues)
    val currentError = error
    LaunchedEffect(currentError) {
        if (!currentError.isNullOrBlank()) {
            onMessage(currentError)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Bienvenido", modifier = Modifier.padding(bottom = 8.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Correo electrónico") },
            modifier = Modifier.fillMaxWidth(0.9f),
            singleLine = true
        )
        fieldErrors["email"]?.let { Text(text = it, color = androidx.compose.material3.MaterialTheme.colorScheme.error, modifier = Modifier.fillMaxWidth(0.9f).padding(top = 4.dp)) }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            modifier = Modifier.fillMaxWidth(0.9f),
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password"
                    )
                }
            }
        )
        fieldErrors["password"]?.let { Text(text = it, color = androidx.compose.material3.MaterialTheme.colorScheme.error, modifier = Modifier.fillMaxWidth(0.9f).padding(top = 4.dp)) }

        val errorValue = error
        if (errorValue != null) {
            val friendlyError = if (errorValue.contains("connect", true) || errorValue.contains("failed", true) || errorValue.contains("timeout", true)) {
                "No se pudo conectar con el servidor"
            } else {
                "Ocurrió un error, intenta más tarde"
            }
            Text(text = friendlyError, modifier = Modifier.padding(top = 8.dp))
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                vm.login(email, password) {
                    onMessage("Inicio de sesión exitoso")
                    onLoginSuccess()
                }
            },
            enabled = !loading,
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp))
            } else {
                Text("Iniciar sesión")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = onGoToRegister, enabled = !loading, modifier = Modifier.fillMaxWidth(0.9f)) {
            Text("Registrarse")
        }
    }
}
