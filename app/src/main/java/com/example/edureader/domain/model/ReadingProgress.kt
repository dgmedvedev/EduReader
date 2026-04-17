package com.example.edureader.domain.model

import java.time.Instant

data class ReadingProgress(
    val bookId: BookId,
    val locator: BookLocator,
    val updatedAt: Instant
)
