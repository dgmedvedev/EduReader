package com.example.edureader.domain.usecase

import com.example.edureader.domain.common.DomainError
import com.example.edureader.domain.common.DomainResult
import com.example.edureader.domain.model.BookId
import com.example.edureader.domain.model.EpubBook
import com.example.edureader.domain.repository.EpubRepository
import com.example.edureader.domain.service.EpubImportSourceResolver
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportEpubFromUriUseCaseTest {

    @Test
    fun `returns resolver failure and does not call importer`() = runBlocking {
        val resolver = FakeSourceResolver(
            result = DomainResult.Failure(DomainError.Storage("Cannot copy uri"))
        )
        val repository = FakeEpubRepository()
        val importer = ImportLocalEpubUseCase(repository)
        val useCase = ImportEpubFromUriUseCase(resolver, importer)

        val result = useCase("content://book")

        assertTrue(result is DomainResult.Failure)
        assertEquals(0, repository.importCalls)
        assertEquals("content://book", resolver.lastUri)
    }

    @Test
    fun `imports copied local file when resolver succeeds`() = runBlocking {
        val resolver = FakeSourceResolver(
            result = DomainResult.Success("/tmp/local-copy.epub")
        )
        val importerResult = DomainResult.Success(BookId("book-42"))
        val repository = FakeEpubRepository(importerResult)
        val importer = ImportLocalEpubUseCase(repository)
        val useCase = ImportEpubFromUriUseCase(resolver, importer)

        val result = useCase("content://book")

        assertEquals(importerResult, result)
        assertEquals(1, repository.importCalls)
        assertEquals("/tmp/local-copy.epub", repository.lastImportedPath)
    }

    private class FakeSourceResolver(
        private val result: DomainResult<String>
    ) : EpubImportSourceResolver {
        var lastUri: String? = null

        override suspend fun copyUriToLocalFile(uriString: String): DomainResult<String> {
            lastUri = uriString
            return result
        }
    }

    private class FakeEpubRepository(
        private val result: DomainResult<BookId> = DomainResult.Success(BookId("book-1"))
    ) : EpubRepository {
        var importCalls: Int = 0
        var lastImportedPath: String? = null

        override suspend fun importFromLocalPath(filePath: String): DomainResult<BookId> {
            importCalls++
            lastImportedPath = filePath
            return result
        }

        override suspend fun getBook(bookId: BookId): DomainResult<EpubBook> {
            error("Not needed in this test")
        }
    }
}
