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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
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
            .verticalScroll(rememberScrollState())

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
               SessionSummary(uiState.sweatLog)

            }

            LogSummaryUiState.Error -> TODO()
        }
    }
}

@Composable
internal fun SessionSummary(sweatLog: SweatLogView) {
    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            style = MaterialTheme.typography.titleLarge,
            text = "${sweatLog.startTime} - ${sweatLog.endTime} (${sweatLog.summary.elapsedTime})"
        )
    }
    Spacer(Modifier.size(16.dp))
    WorkoutSummary(sweatLog.summary)
    Spacer(Modifier.size(16.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(style = MaterialTheme.typography.titleLarge,
            text = "Workout Segments")
    }
    sweatLog.segments.forEachIndexed { index, segment ->
        HorizontalDivider(color = MaterialTheme.colorScheme.primary, thickness = 2.dp)
        SegmentSummary(segment, index)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.primary, thickness = 2.dp)

}

@Composable
internal fun SegmentSummary(segment: WorkoutSegmentView, index: Int) {
    Spacer(Modifier.size(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(style = MaterialTheme.typography.titleMedium,
            text= "[${index + 1}] Resistance ${segment.data.resistance}")
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(style = MaterialTheme.typography.titleMedium,
            text= " ${segment.startTime} - ${segment.endTime} (${segment.data.elapsedTime})")
    }
    Spacer(Modifier.size(8.dp))
    WorkoutSummary(segment.data)
    Spacer(Modifier.size(8.dp))
}

@Composable
internal fun WorkoutSummary(logData: LogDataView) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(space = 16.dp, alignment = Alignment.Start),
    ) {
        Text("Distance: ${logData.distance}")
        Text("Calories: ${logData.totalEnergy}")
    }
    Spacer(Modifier.size(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("Power: ${logData.minPower} -> ${logData.avgPower} <-${logData.maxPower}")
    }
    Spacer(Modifier.size(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Heart Rate: ${logData.minHeartrate} -> ${logData.avgHeartrate} <-${logData.maxHeartrate}")
    }
    Spacer(Modifier.size(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Speed: ${logData.minSpeed} -> ${logData.avgSpeed} <-${logData.maxSpeed}")
    }
    Spacer(Modifier.size(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Cadence/RPM: ${logData.minCadence} -> ${logData.avgCadence} <-${logData.maxCadence}")
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
