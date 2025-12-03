package com.example.recetasseguras.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.recetasseguras.auth.ConditionDto
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.material3.TextFieldDefaults

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

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Condiciones", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(24.dp))
        
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            label = { Text("Buscar condición...") },
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            singleLine = true,
        )
        Spacer(Modifier.height(8.dp))
        if (search.isBlank()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Escribe para buscar condiciones...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        } else {
            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No se encontraron condiciones.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(filtered) { cond ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = cond.name,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = { onAddCondition(cond) },
                                enabled = !selectedConditions.contains(cond),
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Text("Agregar")
                            }
                        }
                    }
                }
            }
        }
        if (selectedConditions.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text("Condiciones seleccionadas:", style = MaterialTheme.typography.titleSmall)
            LazyColumn(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                items(selectedConditions) { cond ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = cond.name,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = { onRemoveCondition(cond) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text("Quitar", color = MaterialTheme.colorScheme.onError)
                        }
                    }
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
    