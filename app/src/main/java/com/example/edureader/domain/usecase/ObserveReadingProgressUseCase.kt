package com.example.edureader.domain.usecase

import com.example.edureader.domain.model.BookId
import com.example.edureader.domain.model.ReadingProgress
import com.example.edureader.domain.repository.ReadingProgressRepository
import kotlinx.coroutines.flow.Flow

class ObserveReadingProgressUseCase(
    private val readingProgressRepository: ReadingProgressRepository
) {
    operator fun invoke(bookId: BookId): Flow<ReadingProgress?> {
        return readingProgressRepository.observeProgress(bookId)
    }
}
