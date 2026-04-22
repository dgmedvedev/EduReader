package com.example.edureader.data.repository

import com.example.edureader.data.local.BookCatalogLocalDataSource
import com.example.edureader.data.parser.EpubParser
import com.example.edureader.domain.common.DomainError
import com.example.edureader.domain.common.DomainResult
import com.example.edureader.domain.model.BookId
import com.example.edureader.domain.model.EpubBook
import com.example.edureader.domain.repository.EpubRepository
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EpubRepositoryImpl @Inject constructor(
    private val epubParser: EpubParser,
    private val bookCatalogLocalDataSource: BookCatalogLocalDataSource
) : EpubRepository {

    override suspend fun importFromLocalPath(filePath: String): DomainResult<BookId> {
        val file = File(filePath)
        if (!file.exists() || !file.isFile) {
            return DomainResult.Failure(DomainError.NotFound("EPUB file not found: $filePath"))
        }
        if (!file.extension.equals("epub", ignoreCase = true)) {
            return DomainResult.Failure(DomainError.Validation("Only .epub files are supported."))
        }

        return when (val parsed = epubParser.parse(file.absolutePath)) {
            is DomainResult.Success -> {
                val book = parsed.data
                bookCatalogLocalDataSource.saveBookPath(book.id.value, book.filePath)
                DomainResult.Success(book.id)
            }

            is DomainResult.Failure -> parsed
        }
    }

    override suspend fun getBook(bookId: BookId): DomainResult<EpubBook> {
        val filePath = bookCatalogLocalDataSource.getBookPath(bookId.value)
            ?: return DomainResult.Failure(
                DomainError.NotFound("Book is not imported yet for id: ${bookId.value}")
            )

        return epubParser.parse(filePath)
    }
}
