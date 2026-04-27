package com.example.plantpal.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.plantpal.previewProfile
import com.example.plantpal.ui.theme.PlantPalTheme

@Composable
fun ProfileScreen(
    profile: UiUserProfile,
    onSave: (String, String, Boolean, TemperatureUnit) -> Unit,
    onLogout: () -> Unit
) {
    var name by rememberSaveable(profile.name) { mutableStateOf(profile.name) }
    var email by rememberSaveable(profile.email) { mutableStateOf(profile.email) }
    var remindersEnabled by rememberSaveable(profile.remindersEnabled) { mutableStateOf(profile.remindersEnabled) }
    var temperatureUnit by rememberSaveable(profile.temperatureUnit) { mutableStateOf(profile.temperatureUnit) }
    var savedMessage by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    savedMessage = false
                },
                label = { Text("Display name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        item {
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    savedMessage = false
                },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SettingRow(
                        title = "Daily reminders",
                        description = "Simple placeholder setting for the prototype.",
                        checked = remindersEnabled,
                        onCheckedChange = {
                            remindersEnabled = it
                            savedMessage = false
                        }
                    )
                    TemperatureUnitSetting(
                        selectedUnit = temperatureUnit,
                        onSelected = {
                            temperatureUnit = it
                            savedMessage = false
                        }
                    )
                }
            }
        }

        item {
            if (savedMessage) {
                Text(
                    "Profile saved.",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Button(
                onClick = {
                    onSave(name.trim(), email.trim(), remindersEnabled, temperatureUnit)
                    savedMessage = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Settings")
            }
        }
    }
}

@Composable
private fun TemperatureUnitSetting(
    selectedUnit: TemperatureUnit,
    onSelected: (TemperatureUnit) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Temperature unit",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TemperatureUnitButton(
                label = "Celsius",
                selected = selectedUnit == TemperatureUnit.Celsius,
                onClick = { onSelected(TemperatureUnit.Celsius) },
                modifier = Modifier.weight(1f)
            )
            TemperatureUnitButton(
                label = "Fahrenheit",
                selected = selectedUnit == TemperatureUnit.Fahrenheit,
                onClick = { onSelected(TemperatureUnit.Fahrenheit) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TemperatureUnitButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = MaterialTheme.shapes.small,
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
    ) {
        Text(
            label,
            modifier = Modifier.padding(vertical = 10.dp),
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun SettingRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 915)
@Composable
fun ProfileScreenPreview() {
    PlantPalTheme {
        ProfileScreen(
            profile = previewProfile,
            onSave = { _, _, _, _ -> },
            onLogout = { }
        )
    }
}
