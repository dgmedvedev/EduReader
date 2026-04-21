package com.example.edureader.presentation.reader.contract

import com.example.edureader.domain.model.SpineItem
import com.example.edureader.presentation.reader.model.ReaderTocItem

data class ReaderReadyState(
    val title: String,
    val currentChapterIndex: Int,
    val chapters: List<SpineItem>,
    val tocItems: List<ReaderTocItem>,
    val currentChapterFileUrl: String,
    val pendingRestoreProgressionInChapter: Double?
)
