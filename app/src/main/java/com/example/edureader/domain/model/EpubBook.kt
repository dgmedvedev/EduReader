package com.example.edureader.domain.model

data class EpubBook(
    val id: BookId,
    val filePath: String,
    val title: String,
    val language: String?,
    val authors: List<String>,
    val coverImageHref: String?,
    val tableOfContents: List<TocEntry>,
    val spine: List<SpineItem>
)
