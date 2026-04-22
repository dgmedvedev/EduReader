package com.example.edureader.domain.usecase

import com.example.edureader.domain.common.DomainResult
import com.example.edureader.domain.model.BookId
import com.example.edureader.domain.model.ReadingProgress
import com.example.edureader.domain.repository.ReadingProgressRepository

class GetReadingProgressUseCase(
    private val readingProgressRepository: ReadingProgressRepository
) {
    suspend operator fun invoke(bookId: BookId): DomainResult<ReadingProgress?> {
        return readingProgressRepository.getProgress(bookId)
    }
}
