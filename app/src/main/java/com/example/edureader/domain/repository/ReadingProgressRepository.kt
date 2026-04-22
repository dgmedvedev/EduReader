package com.example.edureader.domain.repository

import com.example.edureader.domain.common.DomainResult
import com.example.edureader.domain.model.BookId
import com.example.edureader.domain.model.ReadingProgress
import kotlinx.coroutines.flow.Flow

interface ReadingProgressRepository {
    suspend fun getProgress(bookId: BookId): DomainResult<ReadingProgress?>
    suspend fun getLastOpenedBookId(): DomainResult<BookId?>
    fun observeProgress(bookId: BookId): Flow<ReadingProgress?>
    suspend fun saveProgress(progress: ReadingProgress): DomainResult<Unit>
}
