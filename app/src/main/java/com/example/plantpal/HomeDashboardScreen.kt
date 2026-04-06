package com.example.plantpal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.plantpal.ui.theme.PlantPalTheme

@Composable
fun HomeDashboardScreen(
    profile: UiUserProfile,
    plants: List<UiPlant>,
    onAddPlant: () -> Unit,
    onPlantClick: (Int) -> Unit,
    onProfileClick: () -> Unit
) {
    HomeDashboardContent(
        modifier = Modifier.fillMaxSize(),
        profile = profile,
        plants = plants,
        onAddPlant = onAddPlant,
        onPlantClick = onPlantClick,
        onProfileClick = onProfileClick
    )
}

@Composable
fun HomeDashboardContent(
    modifier: Modifier = Modifier,
    profile: UiUserProfile,
    plants: List<UiPlant>,
    onAddPlant: () -> Unit,
    onPlantClick: (Int) -> Unit,
    onProfileClick: () -> Unit
) {
    val displayName = profile.name.ifBlank { "Grower" }

    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp)
    ) {
        item {
            Text("Collection", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        if (plants.isEmpty()) {
            item {
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
                        Text("Use the + button in the bottom navigation to add your first plant.")
                    }
                }
            }
        } else {
            items(plants) { plant ->
                PlantSummaryCard(
                    plant = plant,
                    onViewDetails = { onPlantClick(plant.id) }
                )
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
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                plant.nickname.ifBlank { plant.name },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text("Species: ${plant.species.ifBlank { "Unknown" }}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Location: ${plant.location}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Light: ${plant.lightNeeds}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Water every ${plant.wateringFrequencyDays} days", color = MaterialTheme.colorScheme.onSurface)
            Text("Last watered: ${formatStoredDate(plant.lastWateredDate)}", color = MaterialTheme.colorScheme.onSurfaceVariant)

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onViewDetails, modifier = Modifier.weight(1f)) {
                    Text("View Details")
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 915)
@Composable
fun HomeDashboardPreview() {
    PlantPalTheme {
        HomeDashboardContent(
            profile = previewProfile,
            plants = previewPlants,
            onAddPlant = { },
            onPlantClick = { },
            onProfileClick = { }
        )
    }
}
