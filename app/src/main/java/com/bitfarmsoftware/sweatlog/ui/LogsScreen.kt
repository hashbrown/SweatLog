package com.bitfarmsoftware.sweatlog.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitfarmsoftware.sweatlog.R
import com.bitfarmsoftware.sweatlog.model.SweatLog

@Composable
fun LogsScreen(
    onLogClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LogsViewModel = hiltViewModel(),
    ) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    LogsScreen(
        uiState,
        onLogClick,
        modifier
        )
}

@Composable
internal fun LogsScreen(
    uiState: LogsUiState,
    onLogClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn {
        when (uiState) {
            LogsUiState.Loading -> Unit
            is LogsUiState.Success -> {
                items(
                    items = uiState.logs,
                    key = { it.id },
                ) { sweatLog ->
                    LogCard(sweatLog, onLogClick)
                }
            }
        }
    }
    MyFloatingActionButton(onClick = {onLogClick("")})
}

@Composable
fun LogCard(log: SweatLog,
            onLogClick: (String) -> Unit) {
    Row(modifier = Modifier
        .padding(all = 8.dp)
        .fillMaxWidth(),
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

        // We keep track if the Device is expanded or not in this
        // variable
        var isExpanded by remember { mutableStateOf(false) }
        // surfaceColor will be updated gradually from one color to the other
        val surfaceColor by animateColorAsState(
            if (isExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
            label = "AnimationState",
        )

        Column(
            modifier = Modifier
                .clickable { isExpanded = !isExpanded }
                .weight(1f),
        ) {
            Text(
                text = log.title,
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))

            Surface(
                shape = MaterialTheme.shapes.medium,
                shadowElevation = 1.dp,
                // surfaceColor color will be changing gradually from primary to surface
                color = surfaceColor,
                // animateContentSize will change the Surface size gradually
                modifier = Modifier
                    .animateContentSize()
                    .padding(1.dp)
            ) {
                Text(
                    text = "ID(${isExpanded}): ${log.id}",
                    modifier = Modifier.padding(all = 4.dp),
                    maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun MyFloatingActionButton(onClick: () -> Unit) {
    FloatingActionButton(
        onClick = onClick,
        shape = CircleShape, // Adjust the shape as needed
        containerColor = MaterialTheme.colorScheme.primary, // Adjust the background color as needed
        contentColor = MaterialTheme.colorScheme.onPrimary, // Adjust the content color as needed
        modifier = Modifier
            .size(56.dp)
            .padding(8.dp),
    ) {
        Icon(Icons.Filled.Add, "Start Log")
    }
}