package com.example.edureader.domain.usecase

import com.example.edureader.domain.common.DomainResult
import com.example.edureader.domain.model.BookId
import com.example.edureader.domain.service.EpubImportSourceResolver

class ImportEpubFromUriUseCase(
    private val sourceResolver: EpubImportSourceResolver,
    private val importLocalEpubUseCase: ImportLocalEpubUseCase
) {
    suspend operator fun invoke(uriString: String): DomainResult<BookId> {
        val localCopyResult = sourceResolver.copyUriToLocalFile(uriString)
        return when (localCopyResult) {
            is DomainResult.Success -> importLocalEpubUseCase(localCopyResult.data)
            is DomainResult.Failure -> localCopyResult
        }
    }
}
