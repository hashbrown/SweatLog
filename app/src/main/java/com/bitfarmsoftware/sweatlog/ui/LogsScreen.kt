package com.bitfarmsoftware.sweatlog.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitfarmsoftware.sweatlog.R

@Composable
fun LogsScreen(
    onLogClick: (String) -> Unit,
    startWorkout: () -> Unit,
    onHandleBackPress: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LogsViewModel = hiltViewModel(),
    ) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    BackHandler(enabled = true) {
        onHandleBackPress()
    }

    LogsScreen(
        uiState,
        onLogClick,
        startWorkout,
        modifier
        )
}


@Composable
internal fun LogsScreen(
    uiState: LogsUiState,
    onLogClick: (String) -> Unit,
    startWorkout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = Modifier.fillMaxHeight()
//            .background(Color.Green)
            .padding(horizontal = 4.dp)

    ) {
//        LazyColumn(modifier = Modifier.background(Color.Blue)){

            when (uiState) {
                LogsUiState.Loading -> {
                    Text("Loading your SweatLogs")
                }
                is LogsUiState.Success -> {
                    if (uiState.logs.isEmpty()) {
                        Text("No SweatLogs yet, time to get going!")
                    } else {
                    LazyColumn() {
                        items(
                            items = uiState.logs,
                            key = { it.id },
                        ) { sweatLog ->
                            LogSummaryCard(sweatLog, onLogClick)
                        }
                    }
                }
            }
        }
        MyFloatingActionButton(
            onClick = { startWorkout() },
            modifier = Modifier.align(Alignment.BottomEnd)
        )
    }
}

@Composable
fun LogSummaryCard(log: SweatLogView,
                   onLogClick: (String) -> Unit
) {
    Row(modifier = Modifier
        .padding(all = 8.dp)
//        .background(Color.Yellow),
        .fillMaxWidth()
        .clickable { onLogClick(log.id) },
        horizontalArrangement = Arrangement.SpaceAround) {
        Image(
            painter = painterResource(R.drawable.bike_on_car),
            contentDescription = "Contact profile picture",
            modifier = Modifier
                .align(Alignment.CenterVertically)
                // Set image size to 40 dp
                .size(48.dp)
                // Clip image to be shaped as a circle
                .clip(CircleShape)
                .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
        )

        // Add a horizontal space between the image and the column
        Spacer(modifier = Modifier.width(8.dp))

        Column(
            modifier = Modifier
                .weight(1f),
        ) {
            Text(
                text = log.title,
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.titleMedium
            )
//            Spacer(modifier = Modifier.height(4.dp).background(Color.Red))

            Text(
                    text = "time: ${log.summary.elapsedTime}  avg power: ${log.summary.avgPower}  calories: ${log.summary.totalEnergy}",
                    modifier = Modifier.padding(all = 4.dp),
                    style = MaterialTheme.typography.bodyMedium
                )

        }
    }
}

@Composable
fun MyFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier) {
    FloatingActionButton(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp), // Adjust the shape as needed
        containerColor = MaterialTheme.colorScheme.primary, // Adjust the background color as needed
        contentColor = MaterialTheme.colorScheme.onPrimary, // Adjust the content color as needed
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 24.dp).then(modifier),
    ) {
//        Icon(Icons.Filled.Add, "Start Log")
        Text(modifier = Modifier
//            .size(56.dp)
            .padding(horizontal = 16.dp, vertical = 4.dp),
            text = "Start Workout")
    }
}


//@Preview
//@Composable
//fun PreviewLogsScreen() {
//
//    val sweatLogViews = SampleData.logsSample.map { it.display() }.toList()
//    val uiState = LogsUiState.Success(sweatLogViews)
//
//    LogsScreen(
//        uiState,
//        {}
//    )
//}