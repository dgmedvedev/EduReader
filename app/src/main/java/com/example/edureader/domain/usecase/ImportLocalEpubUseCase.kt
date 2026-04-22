package com.example.edureader.domain.usecase

import com.example.edureader.domain.common.DomainError
import com.example.edureader.domain.common.DomainResult
import com.example.edureader.domain.model.BookId
import com.example.edureader.domain.repository.EpubRepository

class ImportLocalEpubUseCase(
    private val epubRepository: EpubRepository
) {
    suspend operator fun invoke(filePath: String): DomainResult<BookId> {
        if (filePath.isBlank()) {
            return DomainResult.Failure(
                DomainError.Validation("EPUB file path must not be blank.")
            )
        }
        return epubRepository.importFromLocalPath(filePath)
    }
}
