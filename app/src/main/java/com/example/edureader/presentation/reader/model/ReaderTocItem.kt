package com.example.edureader.presentation.reader.model

import com.example.edureader.presentation.reader.contract.TextSpec

data class ReaderTocItem(
    val title: TextSpec,
    val href: String,
    val spineIndex: Int
)
