package com.example.edureader.domain.usecase

import com.example.edureader.domain.model.BookLocator
import com.example.edureader.domain.model.EpubBook
import com.example.edureader.domain.model.ReadingProgress

class ResolveInitialLocatorUseCase {
    operator fun invoke(
        book: EpubBook,
        savedProgress: ReadingProgress?
    ): BookLocator? {
        if (savedProgress != null) return savedProgress.locator

        val firstReadableItem = book.spine.firstOrNull { it.linear }
            ?: book.spine.firstOrNull()
            ?: return null

        return BookLocator(
            href = firstReadableItem.href,
            progressionInResource = 0.0,
            progressInBook = 0.0
        )
    }
}
