package com.example.recetasseguras.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Visibility
import com.example.recetasseguras.auth.FoodDto
import com.example.recetasseguras.auth.RecipeDto

@Composable
fun HomeScreen(
    foods: List<FoodDto>?,
    recipes: List<RecipeDto>?,
    loading: Boolean = false,
    error: String? = null,
    onRecipeClick: (RecipeDto) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Sugerencias", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(24.dp))

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

        // Mostrar loading si los datos aún no se han cargado (son null) o si loading es true
        if (foods == null || recipes == null || loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text("Cargando sugerencias...", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            when (selectedTab) {
                0 -> {
                    if (foods.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No hay comidas seguras disponibles.")
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(foods) { food ->
                                FoodCard(food = food)
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
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(recipes) { recipe ->
                                RecipeCard(
                                    recipe = recipe,
                                    onViewClick = { onRecipeClick(recipe) }
                                )
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

@Composable
fun FoodCard(food: FoodDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Restaurant,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = food.name ?: "Sin nombre",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = food.category ?: "Sin categoría",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun RecipeCard(recipe: RecipeDto, onViewClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Restaurant,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = recipe.title ?: "Sin nombre",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = recipe.description ?: "Sin descripción",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
            Spacer(Modifier.width(8.dp))
            Button(onClick = onViewClick) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = "Ver receta",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("Ver")
            }
        }
    }
}
