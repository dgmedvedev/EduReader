package com.example.edureader.domain.model

data class ReadingProgress(
    val bookId: BookId,
    val locator: BookLocator,
    val updatedAtEpochMillis: Long
)
