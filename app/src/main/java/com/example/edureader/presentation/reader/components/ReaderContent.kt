package com.example.edureader.presentation.reader.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.edureader.R
import com.example.edureader.presentation.common.asString
import com.example.edureader.presentation.reader.contract.ReaderIntent
import com.example.edureader.presentation.reader.contract.ReaderReadyState
import com.example.edureader.presentation.reader.contract.ReaderUiState
import com.example.edureader.presentation.reader.webview.ReaderContentWebView
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderContent(
    state: ReaderReadyState,
    uiState: ReaderUiState,
    onPickBook: () -> Unit,
    onIntent: (ReaderIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) {
        onIntent(ReaderIntent.RestoreCurrentScroll)
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val drawerMenuItems: List<DrawerMenuItemUi> = remember {
        listOf(
            DrawerMenuItemUi(
                iconRes = R.drawable.ic_info,
                titleRes = R.string.reader_drawer_about,
                action = ReaderDrawerAction.About
            ),
            DrawerMenuItemUi(
                iconRes = R.drawable.ic_toc,
                titleRes = R.string.reader_drawer_toc,
                action = ReaderDrawerAction.TableOfContents
            ),
            DrawerMenuItemUi(
                iconRes = R.drawable.ic_file_selection,
                titleRes = R.string.reader_drawer_pick_file,
                action = ReaderDrawerAction.PickFile
            )
        )
    }
    val chapterProgress =
        if (state.chapters.isEmpty()) 0f else (state.currentChapterIndex + 1f) / state.chapters.size

    Box(modifier = modifier.fillMaxSize()) {
        ModalNavigationDrawer(
            modifier = Modifier.graphicsLayer { clip = true },
            drawerState = drawerState,
            gesturesEnabled = drawerState.isOpen,
            drawerContent = {
                ReaderDrawerSheet(
                    title = stringResource(R.string.reader_drawer_menu_title),
                    items = drawerMenuItems,
                    onItemClick = { action ->
                        when (action) {
                            ReaderDrawerAction.About -> {
                                onIntent(ReaderIntent.SetAboutDialogVisible(visible = true))
                            }
                            ReaderDrawerAction.TableOfContents -> {
                                onIntent(ReaderIntent.SetChaptersSheetVisible(visible = true))
                            }
                            ReaderDrawerAction.PickFile -> onPickBook()
                        }
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(dimensionResource(R.dimen.reader_drawer_width))
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_menu),
                                contentDescription = stringResource(R.string.reader_action_open_menu),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = state.title,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    LinearProgressIndicator(
                        progress = { chapterProgress.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                    )
                    Text(
                        text = stringResource(
                            R.string.reader_chapter_counter,
                            state.currentChapterIndex + 1,
                            state.chapters.size
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                ElevatedCard(
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    ReaderContentWebView(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                            .clip(RoundedCornerShape(14.dp)),
                        chapterFileUrl = state.currentChapterFileUrl,
                        pendingRestoreProgressionInChapter = state.pendingRestoreProgressionInChapter,
                        onReportScroll = { scrollY, progressionInChapter ->
                            onIntent(
                                ReaderIntent.ReportScroll(
                                    scrollY = scrollY,
                                    progressionInChapter = progressionInChapter
                                )
                            )
                        },
                        onRestoreApplied = { onIntent(ReaderIntent.RestoreScrollApplied) },
                        onPreviousChapter = { onIntent(ReaderIntent.PreviousChapter) },
                        onNextChapter = { onIntent(ReaderIntent.NextChapter) }
                    )
                }

                if (uiState.showChaptersSheet) {
                    ModalBottomSheet(
                        onDismissRequest = {
                            onIntent(ReaderIntent.SetChaptersSheetVisible(visible = false))
                        }
                    ) {
                        LazyColumn(modifier = Modifier.padding(12.dp)) {
                            itemsIndexed(state.tocItems) { _, item ->
                                val tocTextStyle = MaterialTheme.typography.bodyLarge
                                ListItem(
                                    headlineContent = {
                                        Text(
                                            text = item.title.asString(),
                                            style = tocTextStyle,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    trailingContent = {
                                        Text(
                                            text = "${item.spineIndex + 1}",
                                            style = tocTextStyle
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onIntent(
                                                ReaderIntent.SetChaptersSheetVisible(
                                                    visible = false
                                                )
                                            )
                                            onIntent(
                                                ReaderIntent.OpenTocItem(
                                                    spineIndex = item.spineIndex,
                                                    href = item.href
                                                )
                                            )
                                        }
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }

    if (uiState.showAboutDialog) {
        AlertDialog(
            onDismissRequest = {
                onIntent(ReaderIntent.SetAboutDialogVisible(visible = false))
            },
            title = { Text(stringResource(R.string.reader_about_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.reader_about_book_title_value, state.title),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        stringResource(R.string.reader_about_chapters_value, state.chapters.size)
                    )
                    Text(
                        stringResource(R.string.reader_about_toc_value, state.tocItems.size)
                    )
                    Text(
                        stringResource(
                            R.string.reader_about_current_chapter_value,
                            state.currentChapterIndex + 1
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { onIntent(ReaderIntent.SetAboutDialogVisible(visible = false)) }
                ) {
                    Text(stringResource(R.string.reader_action_close))
                }
            }
        )
    }
}
