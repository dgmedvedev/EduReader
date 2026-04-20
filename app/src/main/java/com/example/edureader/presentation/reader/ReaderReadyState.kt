package com.example.edureader.presentation.reader

import com.example.edureader.domain.model.SpineItem

data class ReaderReadyState(
    val title: String,
    val currentChapterIndex: Int,
    val chapters: List<SpineItem>,
    val tocItems: List<ReaderTocItem>,
    val currentChapterFileUrl: String
)
