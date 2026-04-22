package com.example.edureader.presentation.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.edureader.R
import com.example.edureader.core.monitoring.AppLogger
import com.example.edureader.core.extensions.toTextSpecOrNull
import com.example.edureader.domain.common.DomainResult
import com.example.edureader.domain.model.BookId
import com.example.edureader.domain.model.BookLocator
import com.example.edureader.domain.model.ReadingProgress
import com.example.edureader.domain.model.SpineItem
import com.example.edureader.domain.model.TocEntry
import com.example.edureader.domain.usecase.GetBookUseCase
import com.example.edureader.domain.usecase.GetLastOpenedBookIdUseCase
import com.example.edureader.domain.usecase.GetReadingProgressUseCase
import com.example.edureader.domain.usecase.ImportEpubFromUriUseCase
import com.example.edureader.domain.usecase.ResolveInitialLocatorUseCase
import com.example.edureader.domain.usecase.SaveReadingProgressUseCase
import com.example.edureader.presentation.common.toTextSpec
import com.example.edureader.presentation.reader.contract.ReaderIntent
import com.example.edureader.presentation.reader.contract.ReaderOverlayState
import com.example.edureader.presentation.reader.contract.ReaderReadyState
import com.example.edureader.presentation.reader.contract.ReaderState
import com.example.edureader.presentation.reader.contract.TextSpec
import com.example.edureader.presentation.reader.model.ReaderTocItem
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
internal class ReaderViewModel @Inject constructor(
    private val importEpubFromUriUseCase: ImportEpubFromUriUseCase,
    private val getBookUseCase: GetBookUseCase,
    private val getLastOpenedBookIdUseCase: GetLastOpenedBookIdUseCase,
    private val getReadingProgressUseCase: GetReadingProgressUseCase,
    private val saveReadingProgressUseCase: SaveReadingProgressUseCase,
    private val resolveInitialLocatorUseCase: ResolveInitialLocatorUseCase,
    private val appLogger: AppLogger
) : ViewModel() {

    private val _state = MutableStateFlow<ReaderState>(ReaderState.Idle)
    val state: StateFlow<ReaderState> = _state.asStateFlow()
    private val _overlayState = MutableStateFlow(ReaderOverlayState())
    val overlayState: StateFlow<ReaderOverlayState> = _overlayState.asStateFlow()

    private var currentBookId: BookId? = null
    private var currentExtractedBasePath: String? = null
    private var pendingSaveJob: Job? = null
    private var lastPendingLocator: BookLocator? = null
    private var latestProgressionInChapter: Double? = null

    init {
        restoreLastOpenedBook()
    }

    fun onIntent(intent: ReaderIntent) {
        when (intent) {
            is ReaderIntent.PickedDocument -> importFromUri(intent.uriString)
            is ReaderIntent.OpenTocItem -> openTocItem(intent.spineIndex, intent.href)
            is ReaderIntent.ReportScroll -> persistCurrentLocator(
                scrollY = intent.scrollY,
                progressionInChapter = intent.progressionInChapter,
                debounce = true
            )
            ReaderIntent.OnBackButtonClicked -> showExitDialog()
            ReaderIntent.DismissExitDialog -> hideExitDialog()
            is ReaderIntent.SetChaptersSheetVisible -> updateChaptersSheetVisibility(intent.visible)
            is ReaderIntent.SetAboutDialogVisible -> updateAboutDialogVisibility(intent.visible)

            ReaderIntent.RestoreCurrentScroll -> queueCurrentScrollRestore()
            ReaderIntent.AppBackgrounded -> flushPendingLocator()
            ReaderIntent.RestoreScrollApplied -> clearPendingScrollRestore()

            ReaderIntent.NextChapter -> moveChapter(1)
            ReaderIntent.PreviousChapter -> moveChapter(-1)
        }
    }

    private fun restoreLastOpenedBook() {
        viewModelScope.launch {
            val result = getLastOpenedBookIdUseCase()
            if (result is DomainResult.Failure) {
                appLogger.reportDomainError("ReaderViewModel.restoreLastOpenedBook", result.error)
            }
            val lastBookId = (result as? DomainResult.Success)?.data ?: return@launch
            _state.value = ReaderState.Importing
            currentBookId = lastBookId
            openBook(lastBookId)
        }
    }

    private fun importFromUri(uriString: String) {
        viewModelScope.launch {
            _state.value = ReaderState.Importing

            when (val result = importEpubFromUriUseCase(uriString)) {
                is DomainResult.Success -> {
                    currentBookId = result.data
                    openBook(result.data)
                }

                is DomainResult.Failure -> {
                    appLogger.reportDomainError("ReaderViewModel.importFromUri", result.error)
                    _state.value = ReaderState.Failure(result.error.toTextSpec())
                }
            }
        }
    }

    private fun openBook(bookId: BookId) {
        viewModelScope.launch {
            val bookResult = getBookUseCase(bookId)
            if (bookResult is DomainResult.Failure) {
                appLogger.reportDomainError("ReaderViewModel.openBook.getBook", bookResult.error)
                _state.value = ReaderState.Failure(bookResult.error.toTextSpec())
                return@launch
            }
            val book = (bookResult as DomainResult.Success).data

            val progressResult = getReadingProgressUseCase(bookId)
            val savedProgress = when (progressResult) {
                is DomainResult.Success -> progressResult.data
                is DomainResult.Failure -> {
                    appLogger.reportDomainError(
                        "ReaderViewModel.openBook.getReadingProgress",
                        progressResult.error
                    )
                    null
                }
            }
            val initialLocator = resolveInitialLocatorUseCase(book, savedProgress)
            if (book.spine.isEmpty()) {
                _state.value = ReaderState.Failure(
                    TextSpec.Res(R.string.reader_error_epub_empty_content)
                )
                return@launch
            }
            val currentIndex = book.spine.indexOfFirst { it.href == initialLocator?.href }
                .let { if (it < 0) 0 else it }
            val chapterUrl =
                File(book.extractedBasePath, book.spine[currentIndex].href).toURI().toString()
            currentExtractedBasePath = book.extractedBasePath

            _state.value = ReaderState.Ready(
                ReaderReadyState(
                    title = book.title,
                    currentChapterIndex = currentIndex,
                    chapters = book.spine,
                    tocItems = buildTocItems(
                        chapters = book.spine,
                        tableOfContents = book.tableOfContents
                    ),
                    currentChapterFileUrl = chapterUrl,
                    pendingRestoreProgressionInChapter = initialLocator?.progressionInResource
                )
            )
            latestProgressionInChapter = initialLocator?.progressionInResource
            if (initialLocator != null) {
                persistLocator(initialLocator)
            }
        }
    }

    private fun openChapter(index: Int) {
        navigateToChapter(spineIndex = index, targetHref = null)
    }

    private fun openTocItem(spineIndex: Int, href: String) {
        navigateToChapter(spineIndex = spineIndex, targetHref = href)
    }

    private fun navigateToChapter(spineIndex: Int, targetHref: String?) {
        val ready = (_state.value as? ReaderState.Ready)?.data ?: return
        if (spineIndex !in ready.chapters.indices) return
        val basePath = currentExtractedBasePath ?: return
        val chapterPath = normalizeHref(targetHref ?: ready.chapters[spineIndex].href)
        val fragment = targetHref?.substringAfter('#', "").orEmpty()
        val fileUrl = File(basePath, chapterPath).toURI().toString()
        val targetUrl = if (fragment.isBlank()) fileUrl else "$fileUrl#$fragment"

        _state.value = ReaderState.Ready(
            ready.copy(
                currentChapterIndex = spineIndex,
                currentChapterFileUrl = targetUrl,
                pendingRestoreProgressionInChapter = null
            )
        )
        latestProgressionInChapter = 0.0
        persistCurrentLocator(scrollY = 0, progressionInChapter = 0.0, debounce = false)
    }

    private fun moveChapter(delta: Int) {
        val ready = (_state.value as? ReaderState.Ready)?.data ?: return
        val nextIndex = (ready.currentChapterIndex + delta).coerceIn(ready.chapters.indices)
        openChapter(nextIndex)
    }

    private fun persistCurrentLocator(
        scrollY: Int,
        progressionInChapter: Double,
        debounce: Boolean
    ) {
        if (scrollY < 0) return
        if (progressionInChapter.isNaN()) return
        val ready = (_state.value as? ReaderState.Ready)?.data ?: return
        val chapter = ready.chapters.getOrNull(ready.currentChapterIndex) ?: return
        val progressInBook =
            if (ready.chapters.size <= 1) 0.0 else ready.currentChapterIndex.toDouble() / (ready.chapters.size - 1)

        val locator = BookLocator(
            href = chapter.href,
            progressionInResource = progressionInChapter.coerceIn(0.0, 1.0),
            progressInBook = progressInBook
        )
        latestProgressionInChapter = locator.progressionInResource
        lastPendingLocator = locator
        if (!debounce) {
            persistLocator(locator)
            return
        }
        pendingSaveJob?.cancel()
        pendingSaveJob = viewModelScope.launch {
            delay(500)
            persistLocator(locator)
        }
    }

    private fun flushPendingLocator() {
        val pendingLocator = lastPendingLocator ?: return
        pendingSaveJob?.cancel()
        persistLocator(pendingLocator)
    }

    private fun persistLocator(locator: BookLocator) {
        val bookId = currentBookId ?: return
        viewModelScope.launch {
            val saveResult = saveReadingProgressUseCase(
                ReadingProgress(
                    bookId = bookId,
                    locator = locator,
                    updatedAtEpochMillis = System.currentTimeMillis()
                )
            )
            if (saveResult is DomainResult.Failure) {
                appLogger.reportDomainError("ReaderViewModel.persistLocator", saveResult.error)
            }
        }
    }

    private fun clearPendingScrollRestore() {
        val ready = (_state.value as? ReaderState.Ready)?.data ?: return
        if (ready.pendingRestoreProgressionInChapter == null) return
        _state.value = ReaderState.Ready(
            ready.copy(pendingRestoreProgressionInChapter = null)
        )
    }

    private fun queueCurrentScrollRestore() {
        val ready = (_state.value as? ReaderState.Ready)?.data ?: return
        if (ready.pendingRestoreProgressionInChapter != null) return
        val progressionToRestore = latestProgressionInChapter ?: return

        _state.value = ReaderState.Ready(
            ready.copy(
                pendingRestoreProgressionInChapter = progressionToRestore.coerceIn(0.0, 1.0)
            )
        )
    }

    private fun showExitDialog() {
        _overlayState.value = _overlayState.value.copy(showExitDialog = true)
    }

    private fun hideExitDialog() {
        _overlayState.value = _overlayState.value.copy(showExitDialog = false)
    }

    private fun updateChaptersSheetVisibility(visible: Boolean) {
        _overlayState.value = _overlayState.value.copy(showChaptersSheet = visible)
    }

    private fun updateAboutDialogVisibility(visible: Boolean) {
        _overlayState.value = _overlayState.value.copy(showAboutDialog = visible)
    }

    private fun buildTocItems(
        chapters: List<SpineItem>,
        tableOfContents: List<TocEntry>
    ): List<ReaderTocItem> =
        flattenToc(tableOfContents)
            .mapIndexedNotNull { index, entry ->
                val normalizedEntryHref = normalizeHref(entry.href)
                val spineIndex =
                    chapters.indexOfFirst { normalizeHref(it.href) == normalizedEntryHref }
                if (spineIndex < 0) return@mapIndexedNotNull null

                val title = entry.title.trim()
                ReaderTocItem(
                    title = title.toTextSpecOrNull() ?: chapterFallbackTextSpec(index + 1),
                    href = entry.href,
                    spineIndex = spineIndex
                )
            }
            .ifEmpty {
                chapters.mapIndexed { index, chapter ->
                    ReaderTocItem(
                        title = chapterFallbackTextSpec(index + 1),
                        href = chapter.href,
                        spineIndex = index
                    )
                }
            }

    private fun flattenToc(entries: List<TocEntry>): List<TocEntry> =
        entries.flatMap { entry -> listOf(entry) + flattenToc(entry.children) }

    private fun chapterFallbackTextSpec(chapterNumber: Int): TextSpec =
        TextSpec.Res(
            id = R.string.reader_toc_item_chapter,
            args = listOf(chapterNumber)
        )

    private fun normalizeHref(href: String): String = href.substringBefore('#')
}
