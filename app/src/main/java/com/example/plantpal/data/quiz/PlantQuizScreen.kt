package com.example.plantpal.quiz

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.plantpal.data.local.PlantEntity

@Composable
fun PlantQuizScreen(
    plant: PlantEntity,
    onDone: () -> Unit
) {
    val viewModel: PlantQuizViewModel = viewModel()
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var lastTiltTime by remember { mutableLongStateOf(0L) }

    LaunchedEffect(plant.id) {
        viewModel.startQuiz(plant)
    }

    DisposableEffect(Unit) {
        val sensorManager =
            context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val tiltThreshold = 4.5f
        val cooldownMillis = 900L

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                val values = event?.values ?: return
                val xAxis = values[0]
                val now = SystemClock.elapsedRealtime()

                if (now - lastTiltTime < cooldownMillis) return

                when {
                    xAxis > tiltThreshold -> {
                        lastTiltTime = now
                        viewModel.answerLeft()
                    }

                    xAxis < -tiltThreshold -> {
                        lastTiltTime = now
                        viewModel.answerRight()
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        if (accelerometer != null) {
            sensorManager.registerListener(
                listener,
                accelerometer,
                SensorManager.SENSOR_DELAY_UI
            )
        }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    if (state.completed) {
        val result = state.diagnosisResult

        if (result != null) {
            PlantDiagnosisResultScreen(
                plantName = plant.name,
                result = result,
                onDone = onDone
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Plant Health Result",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text("No result available.")
                Button(onClick = onDone) {
                    Text("Return to dashboard")
                }
            }
        }
        return
    }

    val node = state.currentNode ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = plant.name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = node.question,
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = { viewModel.answerLeft() },
                modifier = Modifier.weight(1f)
            ) {
                Text(node.leftLabel)
            }

            Button(
                onClick = { viewModel.answerRight() },
                modifier = Modifier.weight(1f)
            ) {
                Text(node.rightLabel)
            }
        }

        Text(
            text = "Tap the buttons or tilt left/right to answer.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}