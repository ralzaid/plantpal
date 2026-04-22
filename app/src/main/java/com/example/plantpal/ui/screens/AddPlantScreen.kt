package com.example.plantpal.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.example.plantpal.BuildConfig
import com.example.plantpal.data.local.PerenualSearchResult
import com.example.plantpal.data.local.PerenualService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

@Composable
fun AddPlantScreen(
    onSave: (String, String, PerenualSearchResult?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var placement by remember { mutableStateOf("Indoor") }
    var selectedResult by remember { mutableStateOf<PerenualSearchResult?>(null) }

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

        val apiKey = BuildConfig.PERENUAL_API_KEY.trim()
        if (apiKey.isBlank()) {
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
                apiKey = apiKey,
                query = query
            )
            if (response.isSuccessful) {
                searchResults = response.body()?.data.orEmpty().take(5)
                searchStatus = if (searchResults.isEmpty()) {
                    "No matches found for '$query'."
                } else {
                    "Tap a result to autofill the form."
                }
            } else {
                searchResults = emptyList()
                searchStatus = "Perenual request failed (${response.code()}). Verify API key and quota."
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            e.printStackTrace()
            searchResults = emptyList()
            val ioReason = when (e) {
                is UnknownHostException -> {
                    val hostHint = e.localizedMessage ?: "unknown host"
                    "DNS lookup failed: $hostHint"
                }
                is SocketTimeoutException -> "Connection timed out contacting Perenual."
                is SSLException -> "TLS/SSL handshake failed. Check device date/time and certificates."
                is ConnectException -> "Could not connect to Perenual server."
                else -> e.localizedMessage ?: "unknown network error"
            }
            searchStatus = "Network error: $ioReason"
        } catch (e: Exception) {
            e.printStackTrace()
            searchResults = emptyList()
            searchStatus = "Search failed: ${e.localizedMessage ?: "unknown error"}"
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
                "Add Plant",
                style = MaterialTheme.typography.headlineSmall
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        selectedResult = null
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
                        color = if (selectedResult == null) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
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
                        selectedResult = result
                        name = result.commonName ?: name
                        searchResults = emptyList()
                        searchStatus = "Selected ${result.commonName ?: "plant"}"
                        isSearching = false
                    }
                )
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Placement", style = MaterialTheme.typography.titleSmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PlacementOptionButton(
                        label = "Indoor",
                        selected = placement == "Indoor",
                        onClick = { placement = "Indoor" },
                        modifier = Modifier.weight(1f)
                    )
                    PlacementOptionButton(
                        label = "Outdoor",
                        selected = placement == "Outdoor",
                        onClick = { placement = "Outdoor" },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        item {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(
                            name.trim(),
                            placement,
                            selectedResult
                        )
                    }
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Plant")
            }
        }
    }
}

@Composable
private fun PlacementOptionButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (selected) {
        Button(
            onClick = onClick,
            modifier = modifier
        ) {
            Text(label)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier
        ) {
            Text(label)
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
            modifier = Modifier.padding(12.dp),
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
