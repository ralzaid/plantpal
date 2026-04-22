package com.example.plantpal.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.plantpal.previewPlants
import com.example.plantpal.ui.theme.PlantPalTheme
import java.util.Date
import java.util.concurrent.TimeUnit

@Composable
fun PlantDetailScreen(
    plant: UiPlant?,
    wateringLogs: List<UiWateringLog>,
    onWaterPlant: () -> Unit,
    onHealthCheck: () -> Unit,
    onDelete: () -> Unit
) {
    if (plant == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Plant not found")
        }
        return
    }

    PlantDetailContent(
        plant = plant,
        wateringLogs = wateringLogs,
        onWaterPlant = onWaterPlant,
        onHealthCheck = onHealthCheck,
        onDelete = onDelete
    )
}

@Composable
fun PlantDetailContent(
    plant: UiPlant,
    wateringLogs: List<UiWateringLog>,
    onWaterPlant: () -> Unit,
    onHealthCheck: () -> Unit,
    onDelete: () -> Unit
) {
    val wateredToday = plant.lastWateredDate == todayStorageDate()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = plant.nickname.ifBlank { plant.name },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text("Plant name: ${plant.name}")
                    Text("Species: ${plant.species.ifBlank { "Unknown" }}")
                    Text("Placed: ${plant.location}")
                    Text("Light needs: ${plant.lightNeeds.ifBlank { "Unknown" }}")
                    Text("Water every: ${plant.wateringFrequencyDays} days")
                    Text("Last watered: ${formatStoredDate(plant.lastWateredDate)}")
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Care Instructions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        displayCareInstructions(plant)
                    )
                }
            }
        }

        item {
            Button(onClick = onHealthCheck, modifier = Modifier.fillMaxWidth()) {
                Text("Emergency Health Check")
            }
        }

        item {
            Text(
                "Watering",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    WateringCounter(
                        plant = plant,
                        wateredToday = wateredToday,
                        onWaterPlant = onWaterPlant
                    )

                    Text(
                        "Recent water log",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (wateringLogs.isEmpty()) {
                        Text(
                            "No watering logged yet.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        wateringLogs.take(5).forEach { log ->
                            Text(
                                "- ${formatStoredDate(log.wateredOn)}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        item {
            Button(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                Text("Delete Plant")
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 915)
@Composable
fun PlantDetailScreenPreview() {
    PlantPalTheme {
        PlantDetailContent(
            plant = previewPlants.first(),
            wateringLogs = listOf(UiWateringLog(id = 1, wateredOn = "2026-04-21")),
            onWaterPlant = { },
            onHealthCheck = { },
            onDelete = { }
        )
    }
}

@Composable
private fun WateringCounter(
    plant: UiPlant,
    wateredToday: Boolean,
    onWaterPlant: () -> Unit
) {
    val daysRemaining = daysUntilNextWatering(plant)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "Next Watering in",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "$daysRemaining ${if (daysRemaining == 1L) "day" else "days"}",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                "watering every ${plant.wateringFrequencyDays} days",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Button(
            onClick = onWaterPlant,
            enabled = !wateredToday,
            modifier = Modifier.size(84.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF24C7B7),
                contentColor = Color.White,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Icon(
                imageVector = Icons.Default.WaterDrop,
                contentDescription = "Water today"
            )
        }
    }
}

private fun todayStorageDate(): String {
    return synchronized(storageDateFormat) {
        storageDateFormat.format(Date())
    }
}

private fun daysUntilNextWatering(plant: UiPlant): Long {
    val lastWatered = parseStoredDate(plant.lastWateredDate) ?: return 0
    val elapsedDays = TimeUnit.MILLISECONDS.toDays(Date().time - lastWatered.time).coerceAtLeast(0)
    return (plant.wateringFrequencyDays - elapsedDays).coerceAtLeast(0)
}

private fun displayCareInstructions(plant: UiPlant): String {
    val instructions = plant.careInstructions.trim()
    if (
        instructions.isBlank() ||
        instructions.contains("not available yet", ignoreCase = true) ||
        instructions.contains("can be added later", ignoreCase = true)
    ) {
        return "Watering: Check the top inch of soil and water only when it feels dry.\n\n" +
            "Light: Start with ${plant.lightNeeds.ifBlank { "bright indirect light" }}.\n\n" +
            "Care: Keep the plant in a stable spot with drainage and watch for leaf changes."
    }
    return instructions
}
