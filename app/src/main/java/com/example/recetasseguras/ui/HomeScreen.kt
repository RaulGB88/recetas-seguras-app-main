package com.example.recetasseguras.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
        Text(text = "Comidas seguras", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        if (loading) {
            CircularProgressIndicator()
        } else {
            if (foods.isEmpty()) {
                Text("No hay comidas seguras disponibles.")
            } else {
                foods.forEach { food ->
                    Text(food.name ?: "Sin nombre", style = MaterialTheme.typography.bodyLarge)
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(text = "Recetas seguras", style = MaterialTheme.typography.titleLarge)
            if (recipes.isEmpty()) {
                Text("No hay recetas seguras disponibles.")
            } else {
                recipes.forEach { recipe ->
                    Text(recipe.title ?: "Sin nombre", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        error?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }
    }
}
