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
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onGoToLogin: () -> Unit,
    onMessage: (String) -> Unit,
    modifier: Modifier = Modifier
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

    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    // Validación gestionada por el backend; no mantenemos errores locales aquí

    // Limpio errores por campo al entrar en la pantalla
    LaunchedEffect(Unit) {
        vm.clearFieldErrors()
    }

    // Muestro snackbar para mensajes de error usando el callback proporcionado
    // Solo muestro el snackbar global si no hay errores por campo (evitar duplicados)
    val currentError = error
    val currentFieldErrors = fieldErrors
    LaunchedEffect(currentError, currentFieldErrors) {
        if (!currentError.isNullOrBlank() && currentFieldErrors.isEmpty()) {
            onMessage(currentError)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Crea tu cuenta", modifier = Modifier.padding(bottom = 8.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Nombre de usuario") },
            modifier = Modifier.fillMaxWidth(0.9f),
            singleLine = true
        )
        fieldErrors["username"]?.let { Text(text = it, color = androidx.compose.material3.MaterialTheme.colorScheme.error, modifier = Modifier.fillMaxWidth(0.9f).padding(top = 4.dp)) }

        Spacer(modifier = Modifier.height(8.dp))

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

        error?.let {
            Text(text = it, color = androidx.compose.material3.MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                // Envío directo al backend; la validación y los mensajes los manejo desde el servidor
                vm.register(username, email, password) {
                    onMessage("Registro exitoso")
                    onRegisterSuccess()
                }
            },
            enabled = !loading,
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp))
            } else {
                Text("Registrarse")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = onGoToLogin, modifier = Modifier.fillMaxWidth(0.9f), enabled = !loading) {
            Text("Volver al inicio de sesión")
        }
    }
}
