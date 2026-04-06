package com.example.plantpal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun PlantDetailScreen(
    plant: UiPlant?,
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
        onDelete = onDelete
    )
}

@Composable
fun PlantDetailContent(
    plant: UiPlant,
    onDelete: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (plant.imageUrl != null) {
                        AsyncImage(
                            model = plant.imageUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Text(
                        text = plant.nickname.ifBlank { plant.name },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text("Plant name: ${plant.name}")
                    Text("Species: ${plant.species.ifBlank { "Unknown" }}")
                    Text("Location: ${plant.location}")
                    Text("Light needs: ${plant.lightNeeds}")
                    Text("Water every: ${plant.wateringFrequencyDays} days")
                    Text("Last watered: ${formatStoredDate(plant.lastWateredDate)}")
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Care Instructions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        plant.careInstructions.ifBlank {
                            "Care instructions can be added later. This prototype keeps plant setup lightweight and shows the care section here in the detail view."
                        }
                    )
                }
            }
        }

        item {
            Button(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                Text("Delete Plant")
            }
        }

        item {
            Text(
                "Tracking",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Tracking features will be added later.")
                    Text("For now, this screen shows the plant details and overall structure of the app.")
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 915)
@Composable
fun PlantDetailScreenPreview() {
    MaterialTheme {
        PlantDetailContent(
            plant = previewPlants.first(),
            onDelete = { }
        )
    }
}
