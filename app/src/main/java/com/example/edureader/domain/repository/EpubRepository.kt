package com.example.edureader.domain.repository

import com.example.edureader.domain.common.DomainResult
import com.example.edureader.domain.model.BookId
import com.example.edureader.domain.model.EpubBook

interface EpubRepository {
    suspend fun importFromLocalPath(filePath: String): DomainResult<BookId>
    suspend fun getBook(bookId: BookId): DomainResult<EpubBook>
}
