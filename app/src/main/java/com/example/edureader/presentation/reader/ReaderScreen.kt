package com.example.edureader.presentation.reader

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

@Composable
fun ReaderRoute(
    modifier: Modifier = Modifier,
    viewModel: ReaderViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val pickEpubLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.onIntent(ReaderIntent.PickedDocument(uri.toString()))
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        viewModel.onIntent(ReaderIntent.AppBackgrounded)
    }

    ReaderScreen(
        state = state,
        onPickBook = {
            pickEpubLauncher.launch(
                arrayOf(
                    "application/epub+zip",
                    "application/zip"
                )
            )
        },
        onIntent = viewModel::onIntent,
        modifier = modifier
    )
}

@Composable
private fun ReaderScreen(
    state: ReaderState,
    onPickBook: () -> Unit,
    onIntent: (ReaderIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    when (state) {
        ReaderState.Idle -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Выберите EPUB файл")
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onPickBook) { Text("Открыть EPUB") }
            }
        }

        ReaderState.Importing -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(12.dp))
                Text("Импорт и разбор EPUB...")
            }
        }

        is ReaderState.Failure -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Ошибка: ${state.message}")
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onPickBook) { Text("Выбрать другой EPUB") }
            }
        }

        is ReaderState.Ready -> {
            ReaderContent(
                state = state.data,
                onPickBook = onPickBook,
                onIntent = onIntent,
                modifier = modifier
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun ReaderContent(
    state: ReaderReadyState,
    onPickBook: () -> Unit,
    onIntent: (ReaderIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    var showChapters by remember { mutableStateOf(false) }
    val latestState by rememberUpdatedState(state)
    val latestOnIntent by rememberUpdatedState(onIntent)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(state.title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
        Text(
            text = "Глава ${state.currentChapterIndex + 1}/${state.chapters.size}",
            style = MaterialTheme.typography.bodySmall
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = { showChapters = true }) { Text("Оглавление") }
            Spacer(modifier = Modifier.weight(1f))
            Button(onClick = onPickBook) { Text("Другой файл") }
        }

        AndroidView(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.cacheMode = WebSettings.LOAD_DEFAULT
                    settings.allowFileAccess = true
                    settings.allowFileAccessFromFileURLs = true
                    settings.allowUniversalAccessFromFileURLs = true
                    addJavascriptInterface(
                        object {
                            @JavascriptInterface
                            fun onScroll(y: Float, progressionInChapter: Float) {
                                latestOnIntent(
                                    ReaderIntent.ReportScroll(
                                        scrollY = y.toInt(),
                                        progressionInChapter = progressionInChapter.toDouble()
                                    )
                                )
                            }
                        },
                        "EduReaderBridge"
                    )
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            view?.evaluateJavascript(JS_SCROLL_LISTENER, null)
                            val restoreProgression = latestState.pendingRestoreProgressionInChapter
                            if (restoreProgression != null) {
                                applyRestoreWithRetries(
                                    webView = view,
                                    progressionInChapter = restoreProgression,
                                    onRestoreFinished = {
                                        latestOnIntent(ReaderIntent.RestoreScrollApplied)
                                    }
                                )
                            }
                        }
                    }
                    loadUrl(state.currentChapterFileUrl)
                }
            },
            update = { view ->
                if (view.url != state.currentChapterFileUrl) {
                    view.loadUrl(state.currentChapterFileUrl)
                }
            }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { onIntent(ReaderIntent.PreviousChapter) },
                enabled = state.currentChapterIndex > 0,
                modifier = Modifier.weight(1f)
            ) {
                Text("Назад")
            }
            Button(
                onClick = { onIntent(ReaderIntent.NextChapter) },
                enabled = state.currentChapterIndex < state.chapters.lastIndex,
                modifier = Modifier.weight(1f)
            ) {
                Text("Вперед")
            }
        }

        if (showChapters) {
            ModalBottomSheet(onDismissRequest = { showChapters = false }) {
                LazyColumn(modifier = Modifier.padding(16.dp)) {
                    itemsIndexed(state.tocItems) { _, item ->
                        Button(
                            onClick = {
                                showChapters = false
                                onIntent(
                                    ReaderIntent.OpenTocItem(
                                        spineIndex = item.spineIndex,
                                        href = item.href
                                    )
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = item.title,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Start
                                )
                                Text(
                                    text = "${item.spineIndex + 1}",
                                    textAlign = TextAlign.End
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private const val JS_SCROLL_LISTENER = """
  (function() {
    if (window.__edureader_scroll_listener) return;
    window.__edureader_scroll_listener = true;
    var timeout = null;
    window.addEventListener('scroll', function() {
      if (timeout !== null) clearTimeout(timeout);
      timeout = setTimeout(function() {
        var y = window.scrollY || document.documentElement.scrollTop || 0;
        var doc = document.documentElement || {};
        var body = document.body || {};
        var fullHeight = Math.max(doc.scrollHeight || 0, body.scrollHeight || 0);
        var viewportHeight = window.innerHeight || doc.clientHeight || 0;
        var maxScrollable = Math.max(fullHeight - viewportHeight, 0);
        var progression = maxScrollable > 0 ? (y / maxScrollable) : 0;
        progression = Math.max(0, Math.min(1, progression));
        if (window.EduReaderBridge && window.EduReaderBridge.onScroll) {
          window.EduReaderBridge.onScroll(y, progression);
        }
      }, 250);
    }, { passive: true });
  })();
"""

private fun buildRestoreScrollScript(progressionInChapter: Double): String {
    val normalized = progressionInChapter.coerceIn(0.0, 1.0)
    return """
      (function() {
        var targetProgress = $normalized;
        var doc = document.documentElement || {};
        var body = document.body || {};
        var fullHeight = Math.max(doc.scrollHeight || 0, body.scrollHeight || 0);
        var viewportHeight = window.innerHeight || doc.clientHeight || 0;
        var maxScrollable = Math.max(fullHeight - viewportHeight, 0);
        var targetY = Math.round(maxScrollable * targetProgress);
        window.scrollTo(0, targetY);
      })();
    """.trimIndent()
}

private fun applyRestoreWithRetries(
    webView: WebView?,
    progressionInChapter: Double,
    onRestoreFinished: () -> Unit
) {
    val delaysMs = listOf(0L, 250L, 800L)
    delaysMs.forEachIndexed { index, delayMs ->
        webView?.postDelayed(
            {
                webView.evaluateJavascript(buildRestoreScrollScript(progressionInChapter), null)
                if (index == delaysMs.lastIndex) {
                    onRestoreFinished()
                }
            },
            delayMs
        )
    }
}
