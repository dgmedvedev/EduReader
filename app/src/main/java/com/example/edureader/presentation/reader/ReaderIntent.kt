package com.example.edureader.presentation.reader

sealed interface ReaderIntent {
    data class PickedDocument(val uriString: String) : ReaderIntent
    data class OpenChapter(val spineIndex: Int) : ReaderIntent
    data class ReportScroll(val scrollY: Int) : ReaderIntent
    data object NextChapter : ReaderIntent
    data object PreviousChapter : ReaderIntent
}
