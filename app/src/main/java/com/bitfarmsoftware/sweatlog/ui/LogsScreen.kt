package com.bitfarmsoftware.sweatlog.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
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
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 4.dp)

    ) {
//        LazyColumn(modifier = Modifier.background(Color.Blue)){

            when (uiState) {
                LogsUiState.Loading -> {
                    Text("Loading your SweatLogs",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(16.dp).fillMaxWidth().align(Alignment.Center),
                        textAlign = TextAlign.Center
                    )
                }
                is LogsUiState.Success -> {
                    if (uiState.logs.isEmpty()) {
                        Text("No SweatLogs yet, time to get going!",
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.padding(16.dp).fillMaxWidth().align(Alignment.Center),
                            textAlign = TextAlign.Center
                        )
                    } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
//        .background(Color.Yellow)
        .shadow(1.dp)
        .padding(all = 8.dp)
        .fillMaxWidth()
        .clickable { onLogClick(log.id) },
        horizontalArrangement = Arrangement.SpaceAround) {
        Image(
            painter = painterResource(R.drawable.bike_on_car),
            contentDescription = "Contact profile picture",
            modifier = Modifier
//                .requiredSize(width=20.dp, height = 60.dp),
                .align(Alignment.CenterVertically)
                .size(width=48.dp, height=60.dp)
                // Set image size to 40 dp
//                .sizeIn(maxWidth = 48.dp, minHeight = 48.dp, maxHeight = 140.dp),
                // Clip image to be shaped as a circle
                .clip(RoundedCornerShape(4.dp)),
            contentScale = ContentScale.FillBounds
        )

        // Add a horizontal space between the image and the column
        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = log.title,
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.titleMedium
            )
//            Spacer(modifier = Modifier.height(4.dp).background(Color.Red))
            Row(modifier = Modifier.fillMaxWidth(),
                ){
                Text(text = "Time:", color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.labelLarge,modifier = Modifier.padding(all = 4.dp))
                Text(text = log.summary.elapsedTime, color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.bodyMedium,modifier = Modifier.padding(all = 4.dp))
                Text(text = "  Calories:", color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.labelLarge,modifier = Modifier.padding(all = 4.dp))
                Text(text = log.summary.totalEnergy, color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.bodyMedium,modifier = Modifier.padding(all = 4.dp))
            }

            Row(modifier = Modifier.fillMaxWidth(),
            ){
                Text(text = "Avg Power:", color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.labelLarge,modifier = Modifier.padding(all = 4.dp))
                Text(text = log.summary.avgPower, color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.bodyMedium,modifier = Modifier.padding(all = 4.dp))
                Text(text = "  Avg HeartRate:", color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.labelLarge,modifier = Modifier.padding(all = 4.dp))
                Text(text = log.summary.avgHeartrate, color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.bodyMedium,modifier = Modifier.padding(all = 4.dp))
            }

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


@Preview
@Composable
fun PreviewLogSummaryCard() {
    val log = SweatLogView(
        id = "1",
        title = "Feb 9, 2025 - 8:59AM - Indoor Cycling",
        startTime = "08:59AM",
        endTime = "10:01AM",
        summary = LogDataView(elapsedTime = "01:02:45",
            avgPower = "144", avgHeartrate = "118", totalEnergy = "437"),
        segments = emptyList()
    )

    LogSummaryCard(log, {})
}

@Preview
@Composable
fun PreviewLogsScreen() {

    val sweatLogViews = listOf(
        SweatLogView(
            id = "3",
            title = "Feb 9, 2025 - 8:59AM - Indoor Cycling",
            startTime = "08:59AM",
            endTime = "10:01AM",
            summary = LogDataView(elapsedTime = "01:02:45",
                avgPower = "144", avgHeartrate = "118", totalEnergy = "437"),
            segments = emptyList()
        ),
        SweatLogView(
            id = "2",
            title = "Feb 2, 2025 - 7:15AM - Indoor Cycling",
            startTime = "07:15AM",
            endTime = "7:50AM",
            summary = LogDataView(elapsedTime = "00:35:45",
                avgPower = "185", avgHeartrate = "158", totalEnergy = "305"),
            segments = emptyList()
        ),
        SweatLogView(
            id = "1",
            title = "Jan 27, 2025 - 6:00AM - Indoor Cycling",
            startTime = "06:00AM",
            endTime = "7:01AM",
            summary = LogDataView(elapsedTime = "01:01:20",
                avgPower = "150", avgHeartrate = "124", totalEnergy = "415"),
            segments = emptyList()
        )
    )

    val loadingState = LogsUiState.Loading
    val emptyLogsState = LogsUiState.Success(emptyList())
    val logsState = LogsUiState.Success(sweatLogViews)

    LogsScreen(
        logsState,
        {},
        {},
        Modifier.background(Color.LightGray)
    )
}