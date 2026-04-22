package com.example.edureader.presentation.reader.contract

internal sealed interface ReaderState {
    data object Idle : ReaderState
    data object Importing : ReaderState
    data class Ready(val data: ReaderReadyState) : ReaderState
    data class Failure(val message: TextSpec) : ReaderState
}
