package com.example.recetasseguras.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.recetasseguras.auth.FoodDto
import com.example.recetasseguras.auth.RecipeDto

@Composable
fun HomeScreen(
    foods: List<FoodDto>,
    recipes: List<RecipeDto>,
    loading: Boolean = false,
    error: String? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "Sugerencias", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))

        val tabs = listOf("Comidas seguras", "Recetas seguras")
        var selectedTab by remember { mutableStateOf(0) }

        ScrollableTabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        if (loading) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            when (selectedTab) {
                0 -> {
                    if (foods.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No hay comidas seguras disponibles.")
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(foods) { food ->
                                Text(food.name ?: "Sin nombre", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(vertical = 8.dp))
                            }
                        }
                    }
                }
                1 -> {
                    if (recipes.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No hay recetas seguras disponibles.")
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(recipes) { recipe ->
                                Text(recipe.title ?: "Sin nombre", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(vertical = 8.dp))
                            }
                        }
                    }
                }
            }
        }

        error?.let {
            val friendlyError = if (it.contains("connect", true) || it.contains("failed", true) || it.contains("timeout", true)) {
                "No se pudo conectar con el servidor"
            } else {
                "Ocurrió un error, intenta más tarde"
            }
            Spacer(Modifier.height(8.dp))
            Text(text = friendlyError, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }
    }
}
