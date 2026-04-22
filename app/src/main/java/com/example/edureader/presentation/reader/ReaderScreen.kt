package com.example.edureader.presentation.reader

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.edureader.R
import com.example.edureader.presentation.common.asString
import com.example.edureader.presentation.reader.contract.ReaderIntent
import com.example.edureader.presentation.reader.contract.ReaderOverlayState
import com.example.edureader.presentation.reader.contract.ReaderState
import com.example.edureader.presentation.reader.components.ReaderContent
import com.example.edureader.presentation.theme.EduReaderTheme

@Composable
internal fun ReaderRoute(
    modifier: Modifier = Modifier,
    onCloseApp: () -> Unit = {},
    viewModel: ReaderViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val overlayState by viewModel.overlayState.collectAsStateWithLifecycle()
    val pickEpubLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.onIntent(intent = ReaderIntent.PickedDocument(uriString = uri.toString()))
        }
    }
    val epubMimeType = stringResource(R.string.reader_document_mime_epub)

    LifecycleEventEffect(event = Lifecycle.Event.ON_PAUSE) {
        viewModel.onIntent(intent = ReaderIntent.AppBackgrounded)
    }

    BackHandler {
        viewModel.onIntent(ReaderIntent.OnBackButtonClicked)
    }

    if (overlayState.showExitDialog) {
        AlertDialog(
            onDismissRequest = {
                viewModel.onIntent(ReaderIntent.DismissExitDialog)
            },
            dismissButton = {
                Column(horizontalAlignment = Alignment.Start) {
                    TextButton(
                        onClick = {
                            viewModel.onIntent(ReaderIntent.DismissExitDialog)
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.reader_exit_dialog_continue),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    TextButton(
                        onClick = {
                            viewModel.onIntent(ReaderIntent.DismissExitDialog)
                            pickEpubLauncher.launch(input = arrayOf(epubMimeType))
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.reader_exit_dialog_pick_another),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    TextButton(
                        onClick = {
                            viewModel.onIntent(ReaderIntent.DismissExitDialog)
                            onCloseApp()
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.reader_exit_dialog_close_app),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {}
        )
    }

    ReaderScreen(
        state = state,
        overlayState = overlayState,
        onPickBook = { pickEpubLauncher.launch(input = arrayOf(epubMimeType)) },
        onIntent = viewModel::onIntent,
        modifier = modifier
    )
}

@Composable
private fun ReaderScreen(
    state: ReaderState,
    overlayState: ReaderOverlayState,
    modifier: Modifier = Modifier,
    onPickBook: () -> Unit,
    onIntent: (ReaderIntent) -> Unit
) {
    when (state) {
        ReaderState.Idle -> {
            ReaderStatusPlaceholder(
                title = stringResource(R.string.reader_idle_title),
                subtitle = stringResource(R.string.reader_idle_subtitle),
                actionLabel = stringResource(R.string.reader_drawer_pick_file),
                onActionClick = onPickBook,
                modifier = modifier
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(dimensionResource(R.dimen.reader_placeholder_shape_radius)),
                    modifier = Modifier
                        .size(dimensionResource(R.dimen.reader_placeholder_icon_size))
                        .clip(RoundedCornerShape(dimensionResource(R.dimen.reader_placeholder_shape_radius)))
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_launcher_foreground),
                        contentDescription = stringResource(R.string.app_name),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        ReaderState.Importing -> {
            ReaderStatusPlaceholder(
                title = stringResource(R.string.reader_importing_title),
                subtitle = stringResource(R.string.reader_importing_subtitle),
                modifier = modifier
            ) { CircularProgressIndicator() }
        }

        is ReaderState.Failure -> {
            ReaderStatusPlaceholder(
                title = stringResource(R.string.reader_failure_title),
                subtitle = state.message.asString(),
                actionLabel = stringResource(R.string.reader_action_pick_another),
                onActionClick = onPickBook,
                modifier = modifier
            ) {}
        }

        is ReaderState.Ready -> {
            ReaderContent(
                state = state.data,
                overlayState = overlayState,
                onPickBook = onPickBook,
                onIntent = onIntent,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun ReaderStatusPlaceholder(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    leading: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = dimensionResource(R.dimen.reader_placeholder_horizontal_padding)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = dimensionResource(R.dimen.reader_placeholder_min_height))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(dimensionResource(R.dimen.reader_placeholder_content_padding)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.reader_placeholder_content_spacing))
            ) {
                leading()
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                if (actionLabel != null && onActionClick != null) {
                    FilledTonalButton(onClick = onActionClick) {
                        Text(text = actionLabel)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ReaderScreenIdlePreview() {
    EduReaderTheme {
        ReaderScreen(
            state = ReaderState.Idle,
            overlayState = ReaderOverlayState(),
            onPickBook = {},
            onIntent = {}
        )
    }
}
