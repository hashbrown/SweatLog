package com.bitfarmsoftware.sweatlog.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import timber.log.Timber


@Composable
fun PermissionLauncher(viewModel: LogViewModel = hiltViewModel()) {
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
    viewModel: LogViewModel = hiltViewModel(),
    ) {

    val uiState by viewModel.uiState2.collectAsStateWithLifecycle()
    val finishWorkout: () -> Unit = {
        viewModel.finishWorkout()
        onFinishWorkout()
    }
    WorkoutScreen(
        uiState,
        finishWorkout
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun WorkoutScreen(
    uiState: LogUiState,
//    onShowSnackbar: suspend (String, String?) -> Boolean,
    finishWorkout: () -> Unit
 //   modifier: Modifier = Modifier,
) {
    Column (
        modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)

    ){

        when (uiState) {
            is LogUiState.Loading -> {
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
            is LogUiState.NeedsPermissions -> {
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
            is LogUiState.Success -> {
//                Timber.i("Re-compose with SUCCESS state")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    WorkoutDataCard("Time Elapsed", uiState.logData.elapsedTime)
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
                        WorkoutDataCard("Distance (Miles)", uiState.logData.distance)
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        WorkoutDataCard("Calories (kCal)", uiState.logData.totalEnergy)
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        WorkoutDataCard("Resistance", uiState.logData.resistance)
                    }
                }
                Spacer(Modifier.size(16.dp))
                Button(onClick = {finishWorkout()},
                        modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text(text = "Finish Workout")
                }

            }
        }
    }
}

@Composable
fun WorkoutDataCard(dataName: String, dataValue: String) {
    Card (border = BorderStroke(1.dp,MaterialTheme.colorScheme.secondary),
        modifier = Modifier.widthIn(min = 20.dp, max = 250.dp)
//        .heightIn(min = 80.dp, max = 150.dp)
//        .fillMaxSize()
        )

    {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = dataValue,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.displayMedium,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.height(16.dp))

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


@Preview
@Composable
fun WorkoutDataCardPreview() {
    WorkoutDataCard("Heart Rate (bpm)", "145")
}

@Preview
@Composable
fun WorkoutScreenPreview() {

    val uiState = LogUiState.Success(LogDataView(power = "130", speed = "15.3", cadence="43", distance = "12.67",
        resistance = "6", totalEnergy = "88", heartrate = "140", elapsedTime = "0:15:43"))
    WorkoutScreen(uiState, {})
}

