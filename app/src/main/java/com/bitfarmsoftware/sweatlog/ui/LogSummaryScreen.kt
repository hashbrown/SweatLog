package com.bitfarmsoftware.sweatlog.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import timber.log.Timber




@Composable
fun LogSummaryScreen(
    modifier: Modifier = Modifier,
    viewModel: LogSummaryViewModel = hiltViewModel(),
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LogSummaryScreen(
        uiState
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LogSummaryScreen(
    uiState: LogSummaryUiState,
) {
    Column (
        modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)

    ){

        when (uiState) {
            is LogSummaryUiState.Loading -> {
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
            is LogSummaryUiState.Success -> {
//                Timber.i("Re-compose with SUCCESS state")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    Text("Resistance: ${uiState.sweatLog.logData.resistance}")
                    Text("Duration: ${uiState.sweatLog.logData.elapsedTime}")
                }
                Spacer(Modifier.size(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    Text("Distance: ${uiState.sweatLog.logData.distance}")
                    Text("Calories: ${uiState.sweatLog.logData.totalEnergy}")
                }
                Spacer(Modifier.size(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    Text("Power: ${uiState.sweatLog.logData.minPower} -> ${uiState.sweatLog.logData.avgPower} <-${uiState.sweatLog.logData.maxPower}")
                }
                Spacer(Modifier.size(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    Text("Heart Rate: ${uiState.sweatLog.logData.minHeartrate} -> ${uiState.sweatLog.logData.avgHeartrate} <-${uiState.sweatLog.logData.maxHeartrate}")
                }
                Spacer(Modifier.size(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    Text("Speed: ${uiState.sweatLog.logData.minSpeed} -> ${uiState.sweatLog.logData.avgSpeed} <-${uiState.sweatLog.logData.maxSpeed}")
                }
                Spacer(Modifier.size(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    Text("Cadence/RPM: ${uiState.sweatLog.logData.minCadence} -> ${uiState.sweatLog.logData.avgCadence} <-${uiState.sweatLog.logData.maxCadence}")
                }


            }

            LogSummaryUiState.Error -> TODO()
        }
    }
}



//@Preview
//@Composable
//fun WorkoutDataCardPreview() {
//    WorkoutDataCard("Heart Rate (bpm)", "145")
//}
//
//@Preview
//@Composable
//fun WorkoutScreenPreview() {
//
//    val uiState = WorkoutUiState.Success(LogDataView(power = "130", speed = "15.3", cadence="43", distance = "12.67",
//        resistance = "6", totalEnergy = "88", heartrate = "140", elapsedTime = "0:15:43"))
//    WorkoutScreen(uiState, {})
//}
//
