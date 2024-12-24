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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitfarmsoftware.sweatlog.R
import com.bitfarmsoftware.sweatlog.model.LogData
import com.bitfarmsoftware.sweatlog.model.SweatLog

@Composable
fun LogScreen(
    modifier: Modifier = Modifier,
    viewModel: LogViewModel = hiltViewModel(),
    ) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LogScreen(
        uiState
    )
}

@Composable
internal fun LogScreen(
    uiState: LogUiState,
//    onShowSnackbar: suspend (String, String?) -> Boolean,
//    onLogClick: (String) -> Unit,
 //   modifier: Modifier = Modifier,
) {
    Column {
        when (uiState) {
            LogUiState.Loading -> Unit
            is LogUiState.Success -> {
                Row {
                    LogDataCard("Power (Watts)", uiState.logData.power.toString())
                    LogDataCard("Speed (Mph)", uiState.logData.speed.toString())
                }

            }
        }
    }
}

@Composable
fun LogDataCard(dataName: String, dataValue: String) {
    Column(
    ) {
        Text(
            text = dataValue,
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.height(4.dp))

        Surface(
            //shape = MaterialTheme.shapes.medium,
           // shadowElevation = 1.dp,
            // surfaceColor color will be changing gradually from primary to surface
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Text(
                text = dataName,
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(all = 4.dp),
            )
        }
    }
}

@Preview
@Composable
fun LogScreenPreview() {

    val uiState = LogUiState.Success(LogData(power = 130, speed = 15.3))
    LogScreen(uiState)
}