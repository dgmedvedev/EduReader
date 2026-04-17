package com.example.edureader.domain.usecase

import com.example.edureader.domain.common.DomainResult
import com.example.edureader.domain.model.ReadingProgress
import com.example.edureader.domain.repository.ReadingProgressRepository

class SaveReadingProgressUseCase(
    private val readingProgressRepository: ReadingProgressRepository
) {
    suspend operator fun invoke(progress: ReadingProgress): DomainResult<Unit> {
        return readingProgressRepository.saveProgress(progress)
    }
}
