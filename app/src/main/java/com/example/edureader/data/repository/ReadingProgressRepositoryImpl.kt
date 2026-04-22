package com.example.edureader.data.repository

import com.example.edureader.data.local.ReadingProgressLocalDataSource
import com.example.edureader.domain.common.DomainError
import com.example.edureader.domain.common.DomainResult
import com.example.edureader.domain.model.BookId
import com.example.edureader.domain.model.ReadingProgress
import com.example.edureader.domain.repository.ReadingProgressRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class ReadingProgressRepositoryImpl @Inject constructor(
    private val localDataSource: ReadingProgressLocalDataSource
) : ReadingProgressRepository {

    override suspend fun getProgress(bookId: BookId): DomainResult<ReadingProgress?> {
        return runCatching { localDataSource.get(bookId) }
            .fold(
                onSuccess = { DomainResult.Success(it) },
                onFailure = {
                    DomainResult.Failure(
                        DomainError.Storage(
                            message = "Failed to load reading progress.",
                            cause = it
                        )
                    )
                }
            )
    }

    override suspend fun getLastOpenedBookId(): DomainResult<BookId?> {
        return runCatching { localDataSource.getLastOpenedBookId() }
            .fold(
                onSuccess = { DomainResult.Success(it) },
                onFailure = {
                    DomainResult.Failure(
                        DomainError.Storage(
                            message = "Failed to load last opened book id.",
                            cause = it
                        )
                    )
                }
            )
    }

    override fun observeProgress(bookId: BookId): Flow<ReadingProgress?> {
        return localDataSource.observe(bookId)
    }

    override suspend fun saveProgress(progress: ReadingProgress): DomainResult<Unit> {
        return runCatching { localDataSource.save(progress) }
            .fold(
                onSuccess = { DomainResult.Success(Unit) },
                onFailure = {
                    DomainResult.Failure(
                        DomainError.Storage(
                            message = "Failed to save reading progress.",
                            cause = it
                        )
                    )
                }
            )
    }
}
