package com.example.edureader.domain.usecase

import com.example.edureader.domain.common.DomainResult
import com.example.edureader.domain.model.BookId
import com.example.edureader.domain.model.BookLocator
import com.example.edureader.domain.model.EpubBook
import com.example.edureader.domain.model.ReadingProgress
import com.example.edureader.domain.model.SpineItem
import com.example.edureader.domain.model.TocEntry
import com.example.edureader.domain.repository.EpubRepository
import com.example.edureader.domain.repository.ReadingProgressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class DelegatingUseCasesTest {

    @Test
    fun `GetBookUseCase delegates to repository`() = runBlocking {
        val expected = DomainResult.Success(testBook())
        val repository = FakeEpubRepository(getBookResult = expected)
        val useCase = GetBookUseCase(repository)
        val bookId = BookId("book-1")

        val result = useCase(bookId)

        assertEquals(expected, result)
        assertEquals(bookId, repository.lastRequestedBookId)
    }

    @Test
    fun `GetReadingProgressUseCase delegates to repository`() = runBlocking {
        val bookId = BookId("book-1")
        val expectedProgress = ReadingProgress(
            bookId = bookId,
            locator = BookLocator("chapter-1.xhtml", 0.5, 0.4),
            updatedAtEpochMillis = 10L
        )
        val expected = DomainResult.Success(expectedProgress)
        val repository = FakeReadingProgressRepository(getProgressResult = expected)
        val useCase = GetReadingProgressUseCase(repository)

        val result = useCase(bookId)

        assertEquals(expected, result)
        assertEquals(bookId, repository.lastProgressRequestBookId)
    }

    @Test
    fun `SaveReadingProgressUseCase delegates to repository`() = runBlocking {
        val progress = ReadingProgress(
            bookId = BookId("book-1"),
            locator = BookLocator("chapter-2.xhtml", 0.3, 0.2),
            updatedAtEpochMillis = 20L
        )
        val expected = DomainResult.Success(Unit)
        val repository = FakeReadingProgressRepository(saveProgressResult = expected)
        val useCase = SaveReadingProgressUseCase(repository)

        val result = useCase(progress)

        assertEquals(expected, result)
        assertEquals(progress, repository.lastSavedProgress)
    }

    @Test
    fun `GetLastOpenedBookIdUseCase delegates to repository`() = runBlocking {
        val expected = DomainResult.Success(BookId("book-2"))
        val repository = FakeReadingProgressRepository(lastOpenedBookIdResult = expected)
        val useCase = GetLastOpenedBookIdUseCase(repository)

        val result = useCase()

        assertEquals(expected, result)
        assertEquals(1, repository.getLastOpenedCalls)
    }

    @Test
    fun `ObserveReadingProgressUseCase delegates flow from repository`() = runBlocking {
        val bookId = BookId("book-3")
        val expectedProgress = ReadingProgress(
            bookId = bookId,
            locator = BookLocator("chapter-3.xhtml", 0.8, 0.75),
            updatedAtEpochMillis = 30L
        )
        val repository = FakeReadingProgressRepository(
            observeProgressFlow = flowOf(expectedProgress)
        )
        val useCase = ObserveReadingProgressUseCase(repository)

        val firstEmission = useCase(bookId).first()

        assertEquals(expectedProgress, firstEmission)
        assertEquals(bookId, repository.lastObservedBookId)
    }

    private class FakeEpubRepository(
        private val importResult: DomainResult<BookId> = DomainResult.Success(BookId("imported-id")),
        private val getBookResult: DomainResult<EpubBook> = DomainResult.Success(testBook())
    ) : EpubRepository {
        var lastRequestedBookId: BookId? = null

        override suspend fun importFromLocalPath(filePath: String): DomainResult<BookId> = importResult

        override suspend fun getBook(bookId: BookId): DomainResult<EpubBook> {
            lastRequestedBookId = bookId
            return getBookResult
        }
    }

    private class FakeReadingProgressRepository(
        private val getProgressResult: DomainResult<ReadingProgress?> = DomainResult.Success(null),
        private val lastOpenedBookIdResult: DomainResult<BookId?> = DomainResult.Success(null),
        private val saveProgressResult: DomainResult<Unit> = DomainResult.Success(Unit),
        private val observeProgressFlow: Flow<ReadingProgress?> = flowOf(null)
    ) : ReadingProgressRepository {
        var lastProgressRequestBookId: BookId? = null
        var lastObservedBookId: BookId? = null
        var lastSavedProgress: ReadingProgress? = null
        var getLastOpenedCalls: Int = 0

        override suspend fun getProgress(bookId: BookId): DomainResult<ReadingProgress?> {
            lastProgressRequestBookId = bookId
            return getProgressResult
        }

        override suspend fun getLastOpenedBookId(): DomainResult<BookId?> {
            getLastOpenedCalls++
            return lastOpenedBookIdResult
        }

        override fun observeProgress(bookId: BookId): Flow<ReadingProgress?> {
            lastObservedBookId = bookId
            return observeProgressFlow
        }

        override suspend fun saveProgress(progress: ReadingProgress): DomainResult<Unit> {
            lastSavedProgress = progress
            return saveProgressResult
        }
    }

    companion object {
        private fun testBook(): EpubBook =
            EpubBook(
                id = BookId("book-1"),
                filePath = "/tmp/book.epub",
                extractedBasePath = "/tmp/book",
                title = "Book",
                language = "en",
                authors = listOf("Author"),
                coverImageHref = null,
                tableOfContents = listOf(TocEntry(id = null, title = "Chapter", href = "chapter-1.xhtml")),
                spine = listOf(
                    SpineItem(
                        idRef = "item-1",
                        href = "chapter-1.xhtml",
                        mediaType = "application/xhtml+xml",
                        linear = true
                    )
                )
            )
    }
}
