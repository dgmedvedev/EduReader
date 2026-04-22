package com.example.edureader.presentation.reader.webview

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.math.abs

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ReaderContentWebView(
    chapterFileUrl: String,
    pendingRestoreProgressionInChapter: Double?,
    modifier: Modifier = Modifier,
    onReportScroll: (scrollY: Int, progressionInChapter: Double) -> Unit,
    onRestoreApplied: () -> Unit,
    onPreviousChapter: () -> Unit,
    onNextChapter: () -> Unit
) {
    val latestChapterUrl = rememberUpdatedState(chapterFileUrl)
    val latestPendingRestore = rememberUpdatedState(pendingRestoreProgressionInChapter)
    val latestOnReportScroll = rememberUpdatedState(onReportScroll)
    val latestOnRestoreApplied = rememberUpdatedState(onRestoreApplied)
    val latestOnPreviousChapter = rememberUpdatedState(onPreviousChapter)
    val latestOnNextChapter = rememberUpdatedState(onNextChapter)

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = true

                val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
                val tapTimeoutMs = ViewConfiguration.getTapTimeout().times(2)
                var downX = 0f
                var downY = 0f
                var downAtMs = 0L

                addJavascriptInterface(
                    object {
                        @JavascriptInterface
                        fun onScroll(y: Float, progressionInChapter: Float) {
                            latestOnReportScroll.value(
                                y.toInt(),
                                progressionInChapter.toDouble()
                            )
                        }
                    },
                    "EduReaderBridge"
                )

                setOnTouchListener { view, event ->
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            downX = event.x
                            downY = event.y
                            downAtMs = event.eventTime
                        }

                        MotionEvent.ACTION_UP -> {
                            val movedTooMuch =
                                abs(event.x - downX) > touchSlop || abs(event.y - downY) > touchSlop
                            val heldTooLong = event.eventTime - downAtMs > tapTimeoutMs
                            if (!movedTooMuch && !heldTooLong && view.width > 0) {
                                val zone = (event.x / view.width).coerceIn(0f, 0.9999f)
                                when {
                                    zone < 1f / 3f -> latestOnPreviousChapter.value()
                                    zone >= 2f / 3f -> latestOnNextChapter.value()
                                }
                                view.performClick()
                            }
                        }
                    }
                    false
                }

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        view?.evaluateJavascript(READER_JS_SCROLL_LISTENER, null)
                        val restoreProgression = latestPendingRestore.value
                        if (restoreProgression != null) {
                            applyReaderRestoreWithRetries(
                                webView = view,
                                progressionInChapter = restoreProgression,
                                onRestoreFinished = latestOnRestoreApplied.value
                            )
                        }
                    }
                }

                loadUrl(latestChapterUrl.value)
            }
        },
        update = { view ->
            if (view.url != latestChapterUrl.value) {
                view.loadUrl(latestChapterUrl.value)
            }
        }
    )
}
