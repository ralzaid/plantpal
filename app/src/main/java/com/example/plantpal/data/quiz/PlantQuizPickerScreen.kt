package com.example.plantpal.quiz

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.plantpal.ui.screens.UiPlant

@Composable
fun PlantQuizPickerScreen(
    plants: List<UiPlant>,
    onSelectPlant: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            Text(
                "Choose a plant to start a health quiz.",
                style = MaterialTheme.typography.titleMedium
            )
        }

        items(plants) { plant ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectPlant(plant.id) },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(plant.name, style = MaterialTheme.typography.titleMedium)
                    Text(plant.species, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}