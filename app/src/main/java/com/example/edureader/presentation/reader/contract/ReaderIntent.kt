package com.example.edureader.presentation.reader.contract

internal sealed interface ReaderIntent {
    data class PickedDocument(val uriString: String) : ReaderIntent
    data class OpenTocItem(val spineIndex: Int, val href: String) : ReaderIntent
    data class ReportScroll(val scrollY: Int, val progressionInChapter: Double) : ReaderIntent
    data object OnBackButtonClicked : ReaderIntent
    data object DismissExitDialog : ReaderIntent
    data class SetChaptersSheetVisible(val visible: Boolean) : ReaderIntent
    data class SetAboutDialogVisible(val visible: Boolean) : ReaderIntent
    data object RestoreCurrentScroll : ReaderIntent
    data object AppBackgrounded : ReaderIntent
    data object RestoreScrollApplied : ReaderIntent
    data object NextChapter : ReaderIntent
    data object PreviousChapter : ReaderIntent
}
