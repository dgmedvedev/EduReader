package com.example.edureader.data.parser

import com.example.edureader.domain.common.DomainResult
import com.example.edureader.domain.model.EpubBook

interface EpubParser {
    suspend fun parse(filePath: String): DomainResult<EpubBook>
}
