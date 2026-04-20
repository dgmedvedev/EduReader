package com.example.edureader.domain.usecase

import com.example.edureader.domain.common.DomainResult
import com.example.edureader.domain.model.BookId
import com.example.edureader.domain.repository.ReadingProgressRepository

class GetLastOpenedBookIdUseCase(
    private val readingProgressRepository: ReadingProgressRepository
) {
    suspend operator fun invoke(): DomainResult<BookId?> {
        return readingProgressRepository.getLastOpenedBookId()
    }
}
