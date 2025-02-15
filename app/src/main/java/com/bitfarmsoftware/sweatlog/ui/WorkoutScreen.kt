package com.bitfarmsoftware.sweatlog.ui

import android.app.backup.BackupDataOutput
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import timber.log.Timber


@Composable
fun PermissionLauncher(viewModel: WorkoutViewModel = hiltViewModel()) {
    Timber.i("launcher contract=${viewModel.permissionLauncher}")
    val launcher = rememberLauncherForActivityResult(viewModel.permissionLauncher) {
        Timber.i("launch result=${it}")
        viewModel.checkPermissions()
    }

    val currLifeCycleState = LocalLifecycleOwner.current.lifecycle.currentState
    Timber.i("Pre-launch lifecycle state=${currLifeCycleState}")

    LaunchedEffect(true) {
        Timber.i("Launch lifecycle state=${currLifeCycleState}")
        launcher.launch(viewModel.requiredPermissions)
    }
}

@Composable
fun WorkoutScreen(
    onFinishWorkout: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WorkoutViewModel = hiltViewModel(),
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val finishWorkout: () -> Unit = {
        viewModel.finishWorkout()
        onFinishWorkout()
    }

    // keep the screen on during workout
    val currentView = LocalView.current
    DisposableEffect(Unit) {
        currentView.keepScreenOn = true
        onDispose {
            currentView.keepScreenOn = false
        }
    }

    WorkoutScreen(
        uiState,
        finishWorkout
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun WorkoutScreen(
    uiState: WorkoutUiState,
//    onShowSnackbar: suspend (String, String?) -> Boolean,
    finishWorkout: () -> Unit
 //   modifier: Modifier = Modifier,
) {
    Column (
        modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)

    ){

        when (uiState) {
            is WorkoutUiState.Loading -> {
                Timber.i("Re-compose with LOADING state")
                Box(modifier = Modifier.fillMaxHeight(),
                    contentAlignment = Alignment.Center) {
                    Text(
                        text = "Waiting for device",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.displayMedium,
                    )
                }
            }
            is WorkoutUiState.NeedsPermissions -> {
                Timber.i("Re-compose with PERMISSIONS state")
                Box(modifier = Modifier.fillMaxHeight(),
                    contentAlignment = Alignment.Center) {
                    Text(
                        text = "Configure HealthConnect",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.displayMedium,
                    )
                }

                PermissionLauncher()
            }
            is WorkoutUiState.Success -> {
//                Timber.i("Re-compose with SUCCESS state")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        WorkoutDataCard("Time Elapsed", uiState.logData.elapsedTime)
                    }
                    Box(modifier = Modifier.weight(0.6f)) {
                        ZoneDataCard("Zone", uiState.logData.heartrate)
                    }
                }
                Spacer(Modifier.size(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        WorkoutDataCard("Power (Watts)", uiState.logData.power)
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        WorkoutDataCard("Heart Rate (BPM)", uiState.logData.heartrate)
                    }
                }
                Spacer(Modifier.size(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        WorkoutDataCard("Speed (Mph)", uiState.logData.speed)
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        WorkoutDataCard("Cadence (RPM)", uiState.logData.cadence)
                    }
                }
                Spacer(Modifier.size(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        WorkoutDataCard("Miles", uiState.logData.distance)
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        WorkoutDataCard("Calories (kCal)", uiState.logData.totalEnergy)
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        WorkoutDataCard("Resistance", uiState.logData.resistance)
                    }
                }
                Spacer(Modifier.size(48.dp))
                Button(onClick = {finishWorkout()},
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            .align(Alignment.CenterHorizontally)
                            .heightIn(min = 64.dp, max = 100.dp)
                            .widthIn(min = 180.dp, max = 250.dp),
                        shape = RoundedCornerShape(10.dp)
                ) {
                    Text(text = "Finish Workout",
                        style = MaterialTheme.typography.labelLarge)
                }

            }
        }
    }
}

@Composable
fun WorkoutDataCard(dataName: String,
                    dataValue: String,
                    brush: Brush = SolidColor(Color.Transparent)) {

    Card (border = BorderStroke(1.dp,MaterialTheme.colorScheme.secondary),
//        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        colors = CardDefaults.cardColors(),
        modifier = Modifier.widthIn(min = 20.dp, max = 250.dp)

//        .heightIn(min = 80.dp, max = 150.dp)
//        .fillMaxSize()
        )

    {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(brush)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = dataValue,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.displayMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()

            )
            Spacer(modifier = Modifier.height(16.dp))
        }


        Surface(
            color = MaterialTheme.colorScheme.surface,

//            modifier = Modifier.fillMaxHeight()
        ) {
            Text(
                text = dataName,
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(all = 4.dp)
                    .fillMaxWidth()
            )
        }

    }
}

@Composable
fun ZoneDataCard(dataName: String, dataValue: String) {

    val numericValue = dataValue.toFloatOrNull() ?: 0f

    // Identify the current zone by finding the zone where the value lies.
    // If the value is out-of-range, default to the first or last zone.
    val zoneIndex = workoutZones.indexOfFirst { numericValue in it.min..it.max }

    val currentZone = when {
        zoneIndex != -1 -> workoutZones[zoneIndex]
        numericValue < workoutZones.first().min -> workoutZones.first()
        else -> workoutZones.last()
    }

    // Get the animated brush based on the current numeric value.
    val backgroundBrush = zoneBackgroundBrush(dataValue, currentZone)

    WorkoutDataCard(dataName, currentZone.zone.toString(), backgroundBrush)
}


@Preview
@Composable
fun HeartRateInteractivePreview() {
    var heartRate by remember { mutableStateOf(80f) }

    Column {
        Text(text = "Heart Rate: ${heartRate.toInt()} BPM",
            color = Color.White)

        ZoneDataCard("Heart Rate", heartRate.toInt().toString())

        Slider(
            value = heartRate,
            onValueChange = { heartRate = it },
            valueRange = 80f..180f
        )
    }
}



@Preview
@Composable
fun WorkoutDataCardPreview() {
    WorkoutDataCard("Heart Rate (bpm)", "145")
}

@Preview
@Composable
fun WorkoutScreenPreview() {
    val uiState = WorkoutUiState.Success(LogDataView(power = "130", speed = "15.3", cadence="43", distance = "12.6",
        resistance = "6", totalEnergy = "88", heartrate = "128", elapsedTime = "0:15:43"))
    WorkoutScreen(uiState, {})
}

// 1. Define a data class for workout zones9
data class WorkoutZone(
    val zone: Int,
    val min: Float,
    val max: Float,
    val color: Color
)

// Example list of zones (adjust the numeric ranges and colors as needed)
val workoutZones = listOf(
    WorkoutZone(1, 0f, 107f, Color.Blue),
    WorkoutZone(2, 107f, 125f, Color.Green),
    WorkoutZone(3, 125f, 142f, Color.Yellow),
    WorkoutZone(4, 142f, 160f, Color(0xFFFFA500)), // Orange
    WorkoutZone(5, 160f, 200f, Color.Red),
)

/**
 * Computes a Brush (either a solid color or a horizontal gradient) based on the
 * numeric data value (provided as a string) and the workout zones.
 *
 * - When the value is in the middle 50% (25%-75%) of a zone, the entire background
 *   is that zone's color.
 *
 * - When the value is above 75% of the current zone and a next zone exists, the background
 *   transitions from the current zone (left) to the next zone (right).
 *
 * - When the value is below 25% of the current zone and a previous zone exists, the background
 *   transitions from the previous zone (left) to the current zone (right).
 */
@Composable
fun zoneBackgroundBrush(dataValue: String, currentZone: WorkoutZone): Brush {
    // Convert the string to a float (default to 0 if parsing fails)
    val numericValue = dataValue.toFloatOrNull() ?: 0f

    val zoneIndex = currentZone.zone - 1

    // Determine next and previous zones, if available.
    val nextZone = if (zoneIndex != -1 && zoneIndex < workoutZones.lastIndex) {
        workoutZones[zoneIndex + 1]
    } else null

    val prevZone = if (zoneIndex > 0) {
        workoutZones[zoneIndex - 1]
    } else null

    // Compute how far the numericValue is through the current zone (0 to 1).
    val zoneProgress = ((numericValue - currentZone.min) / (currentZone.max - currentZone.min))
        .coerceIn(0f, 1f)

    // When in the middle 80% (between 10% and 90%), use a solid color.
    if (zoneProgress in 0.20f..0.80f) {
        return SolidColor(currentZone.color)
    }
    // If the value is above 75% and there is a next zone, transition upward.
    else if (zoneProgress > 0.80f && nextZone != null) {
        // Compute a ratio (0 at 90%, 1 at 100%)
        val upwardTargetRatio = ((zoneProgress - 0.80f) / 0.80f).coerceIn(0f, 1f)
        val animatedUpRatio by animateFloatAsState(targetValue = upwardTargetRatio)
        // Here the split is computed so that when animatedUpRatio is 0 the full background is
        // currentZone.color, and when animatedUpRatio is 1, it becomes nextZone.color.
        val split = 1f - (animatedUpRatio + 0.15f)

        return Brush.horizontalGradient(
            colorStops = arrayOf(
                0.0f to currentZone.color,
                split to currentZone.color,
                split to nextZone.color,
                1.0f to nextZone.color
            )
        )
    }
    // If the value is below 25% and there is a previous zone, transition downward.
    else if (zoneProgress < 0.20f && prevZone != null) {
        // Compute a ratio (0 at 10%, 1 at 0)
        val downwardTargetRatio = ((0.20f - zoneProgress) / 0.80f).coerceIn(0f, 1f)
        val animatedDownRatio by animateFloatAsState(targetValue = downwardTargetRatio)
        // For downward transition:
        // When animatedDownRatio is 0, the full background is currentZone.color.
        // When animatedDownRatio is 1, the full background becomes prevZone.color.
        val split = animatedDownRatio + 0.15f
        return Brush.horizontalGradient(
            colorStops = arrayOf(
                0.0f to prevZone.color,
                split to prevZone.color,
                split to currentZone.color,
                1.0f to currentZone.color
            )
        )
    }
    // In any other case (for example, if there’s no adjacent zone), use the current zone's color.
    else {
        return SolidColor(currentZone.color)
    }
}

fun zoneProgress(dataValue: String, currentZone: WorkoutZone): Float {
    // Convert the string to a float (default to 0 if parsing fails)
    val numericValue = dataValue.toFloatOrNull() ?: 0f

    val zoneIndex = currentZone.zone - 1

    // Determine next and previous zones, if available.
    val nextZone = if (zoneIndex != -1 && zoneIndex < workoutZones.lastIndex) {
        workoutZones[zoneIndex + 1]
    } else null

    val prevZone = if (zoneIndex > 0) {
        workoutZones[zoneIndex - 1]
    } else null

    // Compute how far the numericValue is through the current zone (0 to 1).
    val zoneProgress = ((numericValue - currentZone.min) / (currentZone.max - currentZone.min))
        .coerceIn(0f, 1f)

    // When in the middle 80% (between 10% and 90%), use a solid color.
    if (zoneProgress in 0.10f..0.90f) {
        return 0.5f
    }
    // If the value is above 75% and there is a next zone, transition upward.
    else if (zoneProgress > 0.90f && nextZone != null) {
        // Compute a ratio (0 at 90%, 1 at 100%)
        val upwardTargetRatio = ((zoneProgress - 0.90f) / 0.90f).coerceIn(0f, 1f)

        return upwardTargetRatio
    }
    // If the value is below 25% and there is a previous zone, transition downward.
    else if (zoneProgress < 0.10f && prevZone != null) {
        // Compute a ratio (0 at 10%, 1 at 0)
        val downwardTargetRatio = ((0.10f - zoneProgress) / 0.10f).coerceIn(0f, 1f)
        return downwardTargetRatio
    }
    // In any other case (for example, if there’s no adjacent zone), use the current zone's color.
    else {
        return 0.0f
    }
}
