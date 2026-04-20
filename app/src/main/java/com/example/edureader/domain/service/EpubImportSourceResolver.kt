package com.example.edureader.domain.service

import com.example.edureader.domain.common.DomainResult

interface EpubImportSourceResolver {
    suspend fun copyUriToLocalFile(uriString: String): DomainResult<String>
}
