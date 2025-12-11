package com.example.recetasseguras.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.recetasseguras.auth.AuthViewModel

@Composable
fun StatsScreen(
    viewModel: AuthViewModel,
    modifier: Modifier = Modifier,
    onMessage: (String) -> Unit = {}
) {
    val stats by viewModel.stats.collectAsState()
    val loading by viewModel.statsLoading.collectAsState()
    val error by viewModel.statsError.collectAsState()

    // Solicito las stats la primera vez que no hay datos ni error ni carga,
    // para evitar llamadas repetidas que provoquen parpadeo en la UI.
    LaunchedEffect(stats, loading, error) {
        if (stats == null && !loading && error.isNullOrBlank()) {
            viewModel.loadStats(onError = { onMessage(it) })
        }
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "Resumen del sistema", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 12.dp))

        if (loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }
        if (!error.isNullOrBlank()) {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(8.dp))
                androidx.compose.material3.Button(onClick = { viewModel.loadStats(onError = { onMessage(it) }) }) {
                    Text(text = "Reintentar")
                }
            }
            return@Column
        }
        val users = stats?.get("users") ?: 0L
        val recipes = stats?.get("recipes") ?: 0L
        val foods = stats?.get("foods") ?: 0L
        val conditions = stats?.get("conditions") ?: 0L

        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(title = "Usuarios", value = users, modifier = Modifier.weight(1f).height(120.dp))
                StatCard(title = "Recetas", value = recipes, modifier = Modifier.weight(1f).height(120.dp))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(title = "Comidas", value = foods, modifier = Modifier.weight(1f).height(120.dp))
                StatCard(title = "Condiciones", value = conditions, modifier = Modifier.weight(1f).height(120.dp))
            }
        }
    }
}

@Composable
private fun StatCard(title: String, value: Long, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(text = value.toString(), fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }
    }
}
