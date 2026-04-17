package com.example.edureader.domain.usecase

import com.example.edureader.domain.common.DomainResult
import com.example.edureader.domain.model.BookId
import com.example.edureader.domain.model.EpubBook
import com.example.edureader.domain.repository.EpubRepository

class GetBookUseCase(
    private val epubRepository: EpubRepository
) {
    suspend operator fun invoke(bookId: BookId): DomainResult<EpubBook> {
        return epubRepository.getBook(bookId)
    }
}
