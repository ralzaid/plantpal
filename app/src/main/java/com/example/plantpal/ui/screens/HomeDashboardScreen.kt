package com.example.plantpal.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.plantpal.previewPlants
import com.example.plantpal.ui.theme.PlantPalTheme

@Composable
fun HomeDashboardScreen(
    plants: List<UiPlant>,
    weather: UiWeatherSummary?,
    temperatureUnit: TemperatureUnit,
    onResearchPlant: () -> Unit,
    onAddPlant: () -> Unit,
    onPlantClick: (Int) -> Unit
) {
    HomeDashboardContent(
        modifier = Modifier.fillMaxSize(),
        plants = plants,
        weather = weather,
        temperatureUnit = temperatureUnit,
        onResearchPlant = onResearchPlant,
        onAddPlant = onAddPlant,
        onPlantClick = onPlantClick
    )
}

@Composable
fun HomeDashboardContent(
    modifier: Modifier = Modifier,
    plants: List<UiPlant>,
    weather: UiWeatherSummary?,
    temperatureUnit: TemperatureUnit,
    onResearchPlant: () -> Unit,
    onAddPlant: () -> Unit,
    onPlantClick: (Int) -> Unit
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SpacerHeight(6)

        PlantResearchSearchBar(onClick = onResearchPlant)

        WeatherStats(
            weather = weather,
            temperatureUnit = temperatureUnit,
            dueCount = plants.count { needsWaterToday(it) }
        )

        Text(
            "Alerts",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium
        )
        AlertCard(plants = plants, weather = weather)

        if (plants.isEmpty()) {
            EmptyPlantCard(onAddPlant = onAddPlant)
        } else {
            Text(
                "My plants",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(end = 20.dp)
            ) {
                items(plants) { plant ->
                    PlantSummaryCard(
                        plant = plant,
                        onViewDetails = { onPlantClick(plant.id) }
                    )
                }
            }
        }

        SpacerHeight(12)
    }
}

@Composable
private fun PlantResearchSearchBar(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Research plants",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                "Research your next plant",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun WeatherStats(
    weather: UiWeatherSummary?,
    temperatureUnit: TemperatureUnit,
    dueCount: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatBlock(
            label = "Temp",
            value = weather?.let { formatTemperature(it.temp, temperatureUnit) } ?: "--",
            modifier = Modifier.weight(1f)
        )
        StatBlock(
            label = "Humidity",
            value = weather?.let { "${it.humidity}%" } ?: "--",
            modifier = Modifier.weight(1f)
        )
        StatBlock(
            label = "Due",
            value = dueCount.toString(),
            modifier = Modifier.weight(1f)
        )
    }
}

private fun formatTemperature(tempCelsius: Double, unit: TemperatureUnit): String {
    return when (unit) {
        TemperatureUnit.Celsius -> "${tempCelsius.toInt()}°C"
        TemperatureUnit.Fahrenheit -> "${((tempCelsius * 9 / 5) + 32).toInt()}°F"
    }
}

@Composable
private fun StatBlock(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(86.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AlertCard(
    plants: List<UiPlant>,
    weather: UiWeatherSummary?
) {
    val alert = buildHomeAlert(plants = plants, weather = weather)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiary)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(56.dp),
                color = Color.White,
                shape = MaterialTheme.shapes.small
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.WaterDrop, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    alert.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    alert.message,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private data class HomeAlert(
    val title: String,
    val message: String
)

private fun buildHomeAlert(
    plants: List<UiPlant>,
    weather: UiWeatherSummary?
): HomeAlert {
    val duePlant = plants.firstOrNull { needsWaterToday(it) }
    if (duePlant != null) {
        return HomeAlert(
            title = "Water ${duePlant.name}",
            message = "${duePlant.name} is due for a water check today."
        )
    }

    val outdoorPlants = plants.filter { it.location.contains("outdoor", ignoreCase = true) }
    val outdoorLabel = when (outdoorPlants.size) {
        0 -> "outdoor plants"
        1 -> outdoorPlants.first().name
        else -> "${outdoorPlants.size} outdoor plants"
    }

    val rain = (weather?.rainMillimetersLastHour ?: 0.0) > 0.0
    if (rain && outdoorPlants.isNotEmpty()) {
        return HomeAlert(
            title = "Rain warning",
            message = "Shelter $outdoorLabel so soil does not get waterlogged."
        )
    }

    val windy = (weather?.windSpeedMetersPerSecond ?: 0.0) >= 10.0
    if (windy && outdoorPlants.isNotEmpty()) {
        return HomeAlert(
            title = "Wind warning",
            message = "Move $outdoorLabel to a protected spot until winds settle."
        )
    }

    val hot = (weather?.temp ?: 0.0) >= 28.0
    if (hot && plants.isNotEmpty()) {
        return HomeAlert(
            title = "Heat warning",
            message = "Hot weather can dry soil faster. Check moisture before watering again."
        )
    }

    return HomeAlert(
        title = "Today",
        message = weather?.recommendation
            ?.takeIf { it.isNotBlank() }
            ?: "No urgent alerts right now"
    )
}

@Composable
private fun EmptyPlantCard(onAddPlant: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                "No plants yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text("Start your collection with a plant name and placement.")
            OutlinedButton(onClick = onAddPlant) {
                Text("Add Plant")
            }
        }
    }
}

@Composable
fun PlantSummaryCard(
    plant: UiPlant,
    onViewDetails: () -> Unit
) {
    Card(
        modifier = Modifier
            .size(width = 204.dp, height = 284.dp)
            .clickable(onClick = onViewDetails),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Eco,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(96.dp)
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    plant.nickname.ifBlank { plant.name },
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    plant.location,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${plant.wateringFrequencyDays}-day water rhythm",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Surface(
                color = if (needsWaterToday(plant)) {
                    Color(0xFFFFEEF0)
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                },
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = if (needsWaterToday(plant)) "Water today" else "On track",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    color = if (needsWaterToday(plant)) {
                        Color(0xFF8A3240)
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    },
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun SpacerHeight(height: Int) {
    Spacer(
        modifier = Modifier
            .height(height.dp)
            .defaultMinSize(minHeight = height.dp)
    )
}

@Preview(showBackground = true, widthDp = 412, heightDp = 915)
@Composable
fun HomeDashboardPreview() {
    PlantPalTheme {
        HomeDashboardContent(
            plants = previewPlants,
            weather = UiWeatherSummary(
                temp = 24.0,
                humidity = 76,
                recommendation = "Conditions are stable."
            ),
            temperatureUnit = TemperatureUnit.Celsius,
            onResearchPlant = { },
            onAddPlant = { },
            onPlantClick = { }
        )
    }
}
