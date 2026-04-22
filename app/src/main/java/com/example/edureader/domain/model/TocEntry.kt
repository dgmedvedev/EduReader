package com.example.edureader.domain.model

data class TocEntry(
    val id: String?,
    val title: String,
    val href: String,
    val children: List<TocEntry> = emptyList()
)
