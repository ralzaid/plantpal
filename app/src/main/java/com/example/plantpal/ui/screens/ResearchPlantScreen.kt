package com.example.plantpal.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.plantpal.BuildConfig
import com.example.plantpal.data.local.PerenualDetails
import com.example.plantpal.data.local.PerenualSearchResult
import com.example.plantpal.data.local.PerenualService
import com.example.plantpal.ui.theme.PlantPalTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.Locale
import javax.net.ssl.SSLException

@Composable
fun ResearchPlantScreen() {
    var query by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<PerenualSearchResult>>(emptyList()) }
    var selectedResult by remember { mutableStateOf<PerenualSearchResult?>(null) }
    var selectedDetails by remember { mutableStateOf<PerenualDetails?>(null) }
    var searchStatus by remember { mutableStateOf<String?>(null) }
    var detailsStatus by remember { mutableStateOf<String?>(null) }
    var isSearching by remember { mutableStateOf(false) }
    var isLoadingDetails by remember { mutableStateOf(false) }

    LaunchedEffect(query) {
        val cleanedQuery = query.trim()
        selectedResult = null
        selectedDetails = null
        detailsStatus = null

        if (cleanedQuery.length < 2) {
            searchResults = emptyList()
            searchStatus = null
            isSearching = false
            return@LaunchedEffect
        }

        val apiKey = BuildConfig.PERENUAL_API_KEY.trim()
        if (apiKey.isBlank()) {
            searchResults = emptyList()
            searchStatus = "Add PERENUAL_API_KEY to local.properties to enable plant research."
            isSearching = false
            return@LaunchedEffect
        }

        isSearching = true
        searchStatus = "Searching Perenual..."
        delay(350)

        try {
            val response = PerenualService.api.searchPlants(
                apiKey = apiKey,
                query = cleanedQuery
            )

            if (response.isSuccessful) {
                searchResults = response.body()?.data.orEmpty().take(8)
                searchStatus = if (searchResults.isEmpty()) {
                    "No matches found for '$cleanedQuery'."
                } else {
                    "Tap a plant to preview its care needs."
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
            searchStatus = "Network error: ${networkMessage(e)}"
        } catch (e: Exception) {
            e.printStackTrace()
            searchResults = emptyList()
            searchStatus = "Search failed: ${e.localizedMessage ?: "unknown error"}"
        } finally {
            isSearching = false
        }
    }

    LaunchedEffect(selectedResult) {
        val result = selectedResult ?: return@LaunchedEffect
        val apiKey = BuildConfig.PERENUAL_API_KEY.trim()
        selectedDetails = null

        if (apiKey.isBlank()) {
            detailsStatus = "Care details need a Perenual API key."
            return@LaunchedEffect
        }

        isLoadingDetails = true
        detailsStatus = "Loading care profile..."

        try {
            val response = PerenualService.api.getPlantDetails(result.id, apiKey)
            selectedDetails = if (response.isSuccessful) response.body() else null
            detailsStatus = if (selectedDetails == null) {
                "Detailed care data is not available for this plant yet."
            } else {
                null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            e.printStackTrace()
            detailsStatus = "Network error: ${networkMessage(e)}"
        } catch (e: Exception) {
            e.printStackTrace()
            detailsStatus = "Details failed: ${e.localizedMessage ?: "unknown error"}"
        } finally {
            isLoadingDetails = false
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
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Research your next plant",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Plant name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                searchStatus?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        selectedResult?.let { result ->
            item {
                PlantCarePreviewCard(
                    result = result,
                    details = selectedDetails,
                    isLoading = isLoadingDetails,
                    status = detailsStatus
                )
            }
        }

        if (isSearching) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        items(searchResults) { result ->
            ResearchResultCard(
                result = result,
                selected = result.id == selectedResult?.id,
                onClick = { selectedResult = result }
            )
        }
    }
}

@Composable
private fun PlantCarePreviewCard(
    result: PerenualSearchResult,
    details: PerenualDetails?,
    isLoading: Boolean,
    status: String?
) {
    val watering = details?.watering?.takeIf { it.isNotBlank() } ?: "Average"
    val wateringDays = wateringFrequencyDaysFor(watering)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    displayName(result, details),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                scientificName(result)?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CareFactTile(
                    label = "Maintenance",
                    value = maintenanceLevel(watering),
                    modifier = Modifier.weight(1f)
                )
                CareFactTile(
                    label = "Water",
                    value = "$wateringDays days",
                    modifier = Modifier.weight(1f)
                )
            }

            CareInfoCard(
                title = "Light",
                body = lightNeeds(details)
            )

            CareInfoCard(
                title = "Care notes",
                body = careNotes(details, displayName(result, details))
            )

            if (isLoading) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            status?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CareFactTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(86.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CareInfoCard(
    title: String,
    body: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(
                body,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 6,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ResearchResultCard(
    result: PerenualSearchResult,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = result.commonName ?: "Unnamed plant",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            scientificName(result)?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "Tap for care preview",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

private fun networkMessage(error: IOException): String {
    return when (error) {
        is UnknownHostException -> "DNS lookup failed."
        is SocketTimeoutException -> "Connection timed out contacting Perenual."
        is SSLException -> "TLS/SSL handshake failed. Check device date/time and certificates."
        is ConnectException -> "Could not connect to Perenual server."
        else -> error.localizedMessage ?: "unknown network error"
    }
}

private fun displayName(
    result: PerenualSearchResult,
    details: PerenualDetails?
): String = details?.commonName ?: result.commonName ?: "Unnamed plant"

private fun scientificName(result: PerenualSearchResult): String? =
    result.scientificName?.firstOrNull()?.takeIf { it.isNotBlank() }

private fun lightNeeds(details: PerenualDetails?): String =
    details?.sunlight
        ?.filter { it.isNotBlank() }
        ?.joinToString(separator = ", ")
        ?.takeIf { it.isNotBlank() }
        ?: "Bright indirect light is a safe starting point until you confirm the plant's needs."

private fun careNotes(
    details: PerenualDetails?,
    plantName: String
): String =
    details?.description
        ?.takeIf { it.isNotBlank() }
        ?: "$plantName is best researched before buying: check light, watering, drainage, and whether its care rhythm fits your space."

private fun wateringFrequencyDaysFor(watering: String?): Int {
    return when (watering?.trim()?.lowercase(Locale.US)) {
        "frequent" -> 3
        "average" -> 7
        "minimum" -> 14
        "none" -> 21
        else -> 7
    }
}

private fun maintenanceLevel(watering: String?): String {
    return when (watering?.trim()?.lowercase(Locale.US)) {
        "frequent" -> "High"
        "average" -> "Medium"
        "minimum", "none" -> "Low"
        else -> "Medium"
    }
}

@Preview(showBackground = true)
@Composable
fun ResearchPlantScreenPreview() {
    PlantPalTheme {
        PlantCarePreviewCard(
            result = PerenualSearchResult(
                id = 1,
                commonName = "Monstera",
                scientificName = listOf("Monstera deliciosa")
            ),
            details = PerenualDetails(
                id = 1,
                commonName = "Monstera",
                description = "A tropical houseplant that prefers bright indirect light and a steady watering rhythm.",
                watering = "Average",
                sunlight = listOf("part shade", "filtered shade")
            ),
            isLoading = false,
            status = null
        )
    }
}
