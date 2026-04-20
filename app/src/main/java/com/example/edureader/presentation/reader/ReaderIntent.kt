package com.example.edureader.presentation.reader

sealed interface ReaderIntent {
    data class PickedDocument(val uriString: String) : ReaderIntent
    data class OpenChapter(val spineIndex: Int) : ReaderIntent
    data class OpenTocItem(val spineIndex: Int, val href: String) : ReaderIntent
    data class ReportScroll(val scrollY: Int, val progressionInChapter: Double) : ReaderIntent
    data object AppBackgrounded : ReaderIntent
    data object RestoreScrollApplied : ReaderIntent
    data object NextChapter : ReaderIntent
    data object PreviousChapter : ReaderIntent
}
