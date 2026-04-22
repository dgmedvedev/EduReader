package com.example.edureader.domain.usecase

import com.example.edureader.domain.common.DomainError
import com.example.edureader.domain.common.DomainResult
import com.example.edureader.domain.model.BookId
import com.example.edureader.domain.model.EpubBook
import com.example.edureader.domain.repository.EpubRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportLocalEpubUseCaseTest {

    @Test
    fun `returns validation failure for blank file path`() = runBlocking {
        val repository = FakeEpubRepository()
        val useCase = ImportLocalEpubUseCase(repository)

        val result = useCase("   ")

        assertTrue(result is DomainResult.Failure)
        val failure = result as DomainResult.Failure
        assertTrue(failure.error is DomainError.Validation)
        assertEquals("EPUB file path must not be blank.", failure.error.message)
        assertEquals(0, repository.importCalls)
    }

    @Test
    fun `delegates to repository for non blank file path`() = runBlocking {
        val expected = DomainResult.Success(BookId("imported-book"))
        val repository = FakeEpubRepository(importResult = expected)
        val useCase = ImportLocalEpubUseCase(repository)

        val result = useCase("/tmp/book.epub")

        assertEquals(expected, result)
        assertEquals(1, repository.importCalls)
        assertEquals("/tmp/book.epub", repository.lastImportedPath)
    }

    private class FakeEpubRepository(
        private val importResult: DomainResult<BookId> = DomainResult.Success(BookId("book-1"))
    ) : EpubRepository {
        var importCalls: Int = 0
        var lastImportedPath: String? = null

        override suspend fun importFromLocalPath(filePath: String): DomainResult<BookId> {
            importCalls++
            lastImportedPath = filePath
            return importResult
        }

        override suspend fun getBook(bookId: BookId): DomainResult<EpubBook> {
            error("Not needed in this test")
        }
    }
}
