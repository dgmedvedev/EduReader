package com.example.edureader.presentation.reader
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.edureader.domain.common.DomainResult
import com.example.edureader.domain.model.BookId
import com.example.edureader.domain.model.BookLocator
import com.example.edureader.domain.model.ReadingProgress
import com.example.edureader.domain.usecase.GetBookUseCase
import com.example.edureader.domain.usecase.GetReadingProgressUseCase
import com.example.edureader.domain.usecase.ImportEpubFromUriUseCase
import com.example.edureader.domain.usecase.ResolveInitialLocatorUseCase
import com.example.edureader.domain.usecase.SaveReadingProgressUseCase
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
class ReaderViewModel @Inject constructor(
    private val importEpubFromUriUseCase: ImportEpubFromUriUseCase,
    private val getBookUseCase: GetBookUseCase,
    private val getReadingProgressUseCase: GetReadingProgressUseCase,
    private val saveReadingProgressUseCase: SaveReadingProgressUseCase,
    private val resolveInitialLocatorUseCase: ResolveInitialLocatorUseCase
) : ViewModel() {

    private val _state = MutableStateFlow<ReaderState>(ReaderState.Idle)
    val state: StateFlow<ReaderState> = _state.asStateFlow()

    private var currentBookId: BookId? = null
    private var currentExtractedBasePath: String? = null
    private var pendingSaveJob: Job? = null

    fun onIntent(intent: ReaderIntent) {
        when (intent) {
            is ReaderIntent.PickedDocument -> importFromUri(intent.uriString)
            is ReaderIntent.OpenChapter -> openChapter(intent.spineIndex)
            is ReaderIntent.ReportScroll -> persistCurrentLocator(
                scrollY = intent.scrollY,
                debounce = true
            )

            ReaderIntent.NextChapter -> moveChapter(1)
            ReaderIntent.PreviousChapter -> moveChapter(-1)
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

                is DomainResult.Failure -> _state.value = ReaderState.Failure(result.error.message)
            }
        }
    }

    private fun openBook(bookId: BookId) {
        viewModelScope.launch {
            val bookResult = getBookUseCase(bookId)
            if (bookResult is DomainResult.Failure) {
                _state.value = ReaderState.Failure(bookResult.error.message)
                return@launch
            }
            val book = (bookResult as DomainResult.Success).data

            val progressResult = getReadingProgressUseCase(bookId)
            val savedProgress = when (progressResult) {
                is DomainResult.Success -> progressResult.data
                is DomainResult.Failure -> null
            }
            val initialLocator = resolveInitialLocatorUseCase(book, savedProgress)
            if (book.spine.isEmpty()) {
                _state.value = ReaderState.Failure("В EPUB не найдено содержимое для чтения.")
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
                    currentChapterFileUrl = chapterUrl
                )
            )
            if (initialLocator != null) {
                persistLocator(initialLocator)
            }
        }
    }

    private fun openChapter(index: Int) {
        val ready = (_state.value as? ReaderState.Ready)?.data ?: return
        if (index !in ready.chapters.indices) return
        val basePath = currentExtractedBasePath ?: return
        val item = ready.chapters[index]
        val url = File(basePath, item.href).toURI().toString()
        _state.value = ReaderState.Ready(
            ready.copy(
                currentChapterIndex = index,
                currentChapterFileUrl = url
            )
        )
        persistCurrentLocator(scrollY = 0, debounce = false)
    }

    private fun moveChapter(delta: Int) {
        val ready = (_state.value as? ReaderState.Ready)?.data ?: return
        val nextIndex = (ready.currentChapterIndex + delta).coerceIn(ready.chapters.indices)
        openChapter(nextIndex)
    }

    private fun persistCurrentLocator(scrollY: Int, debounce: Boolean) {
        if (scrollY < 0) return
        val ready = (_state.value as? ReaderState.Ready)?.data ?: return
        val chapter = ready.chapters.getOrNull(ready.currentChapterIndex) ?: return
        val progressInBook =
            if (ready.chapters.size <= 1) 0.0 else ready.currentChapterIndex.toDouble() / (ready.chapters.size - 1)

        val locator = BookLocator(
            href = chapter.href,
            progressionInResource = 0.0,
            progressInBook = progressInBook
        )
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

    private fun persistLocator(locator: BookLocator) {
        val bookId = currentBookId ?: return
        viewModelScope.launch {
            saveReadingProgressUseCase(
                ReadingProgress(
                    bookId = bookId,
                    locator = locator,
                    updatedAtEpochMillis = System.currentTimeMillis()
                )
            )
        }
    }
}
