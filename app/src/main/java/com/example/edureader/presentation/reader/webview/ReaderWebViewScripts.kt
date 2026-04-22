package com.example.edureader.presentation.reader.webview

import android.webkit.WebView

internal const val READER_JS_SCROLL_LISTENER = """
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

private fun buildReaderRestoreScrollScript(progressionInChapter: Double): String {
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

internal fun applyReaderRestoreWithRetries(
    webView: WebView?,
    progressionInChapter: Double,
    onRestoreFinished: () -> Unit
) {
    val delaysMs = listOf(0L, 250L, 800L)
    delaysMs.forEachIndexed { index, delayMs ->
        webView?.postDelayed(
            {
                webView.evaluateJavascript(buildReaderRestoreScrollScript(progressionInChapter), null)
                if (index == delaysMs.lastIndex) {
                    onRestoreFinished()
                }
            },
            delayMs
        )
    }
}
