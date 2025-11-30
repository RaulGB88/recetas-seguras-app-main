package com.example.recetasseguras.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.recetasseguras.auth.ConditionDto
import androidx.compose.ui.Alignment

@Composable
fun ConditionSelectionScreen(
    allConditions: List<ConditionDto>,
    selectedConditions: List<ConditionDto>,
    onAddCondition: (ConditionDto) -> Unit,
    onRemoveCondition: (ConditionDto) -> Unit,
    onSave: () -> Unit,
    loading: Boolean = false,
    error: String? = null,
    modifier: Modifier = Modifier
) {
        var search by remember { mutableStateOf("") }
        val filtered = remember(search, allConditions) {
            if (search.isBlank()) allConditions
            else allConditions.filter {
                it.name.contains(search, ignoreCase = true)
            }
        }

        Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
            BasicTextField(
                value = search,
                onValueChange = { search = it },
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                decorationBox = { inner ->
                    Box(Modifier.fillMaxWidth().padding(8.dp)) {
                        if (search.isBlank()) Text("Buscar condición...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        inner()
                    }
                }
            )
        Spacer(Modifier.height(8.dp))
        Text(text = "Resultados:", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(4.dp))
        Column(modifier = Modifier.weight(1f)) {
            filtered.forEach { cond ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(cond.name)
                    Button(
                        onClick = { onAddCondition(cond) },
                        enabled = !selectedConditions.contains(cond)
                    ) { Text("Agregar") }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(text = "Seleccionadas:", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(4.dp))
        Column {
            selectedConditions.forEach { cond ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(cond.name)
                    Button(onClick = { onRemoveCondition(cond) }) { Text("Eliminar") }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onSave,
            enabled = selectedConditions.isNotEmpty() && !loading,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Guardar") }
        if (loading) {
            CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
        }
        error?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }
    }
}
