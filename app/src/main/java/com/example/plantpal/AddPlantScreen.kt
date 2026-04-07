package com.example.plantpal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.plantpal.data.local.PerenualSearchResult
import com.example.plantpal.data.local.PerenualService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

@Composable
fun AddPlantScreen(
    onSave: (String, String, String, Int, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var species by remember { mutableStateOf("") }
    var plantType by remember { mutableStateOf("Indoor") }
    var wateringDays by remember { mutableStateOf("7") }
    var careInstructions by remember { mutableStateOf("") }

    var searchResults by remember { mutableStateOf<List<PerenualSearchResult>>(emptyList()) }
    var searchStatus by remember { mutableStateOf<String?>(null) }
    var isSearching by remember { mutableStateOf(false) }
    var skipNextLookup by remember { mutableStateOf(false) }

    LaunchedEffect(name) {
        val query = name.trim()

        if (skipNextLookup) {
            skipNextLookup = false
            return@LaunchedEffect
        }

        if (query.length < 2) {
            searchResults = emptyList()
            searchStatus = null
            isSearching = false
            return@LaunchedEffect
        }

        if (BuildConfig.PERENUAL_API_KEY.isBlank()) {
            println("KEY DEBUG: ${BuildConfig.PERENUAL_API_KEY}")
            searchResults = emptyList()
            searchStatus = "Add PERENUAL_API_KEY to local.properties to enable plant lookup."
            isSearching = false
            return@LaunchedEffect
        }

        isSearching = true
        searchStatus = "Searching Perenual..."
        delay(350)

        try {
            val response = PerenualService.api.searchPlants(
                apiKey = BuildConfig.PERENUAL_API_KEY,
                query = query
            )
            searchResults = response.data.take(5)
            searchStatus = if (searchResults.isEmpty()) {
                "No matches found."
            } else {
                "Tap a result to autofill the form."
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            searchResults = emptyList()
            searchStatus = "Search failed. Check your API key and internet connection."
        } finally {
            isSearching = false
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            Text(
                "Start typing a plant name and matching Perenual results will appear automatically.",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        item {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text("Plant Details", style = MaterialTheme.typography.titleMedium)
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        searchStatus = null
                    },
                    label = { Text("Plant name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                searchStatus?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        if (searchResults.isNotEmpty()) {
            items(searchResults) { result ->
                SearchResultCard(
                    result = result,
                    onUseResult = {
                        skipNextLookup = true
                        name = result.commonName ?: name
                        species = result.scientificName?.firstOrNull().orEmpty()
                        if (careInstructions.isBlank()) {
                            careInstructions = "Follow plant-specific care guidance."
                        }
                        searchResults = emptyList()
                        searchStatus = null
                        isSearching = false
                    }
                )
            }
        }

        item {
            OutlinedTextField(
                value = species,
                onValueChange = { species = it },
                label = { Text("Species") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = plantType,
                onValueChange = { plantType = it },
                label = { Text("Plant type (Indoor/Outdoor)") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = wateringDays,
                onValueChange = { wateringDays = it.filter { ch -> ch.isDigit() } },
                label = { Text("Water every X days") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = careInstructions,
                onValueChange = { careInstructions = it },
                label = { Text("Care instructions") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(
                            name.trim(),
                            species.trim(),
                            plantType.trim(),
                            wateringDays.toIntOrNull() ?: 7,
                            careInstructions.trim()
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Plant")
            }
        }
    }
}

@Composable
private fun SearchResultCard(
    result: PerenualSearchResult,
    onUseResult: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onUseResult),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = result.commonName ?: "Unnamed plant",
                style = MaterialTheme.typography.titleMedium
            )

            val scientificName = result.scientificName?.firstOrNull()
            if (!scientificName.isNullOrBlank()) {
                Text(
                    text = scientificName,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Text(
                text = "Tap to autofill",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}