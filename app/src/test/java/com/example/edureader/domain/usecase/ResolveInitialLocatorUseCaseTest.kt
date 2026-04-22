package com.example.edureader.domain.usecase

import com.example.edureader.domain.model.BookId
import com.example.edureader.domain.model.BookLocator
import com.example.edureader.domain.model.EpubBook
import com.example.edureader.domain.model.ReadingProgress
import com.example.edureader.domain.model.SpineItem
import com.example.edureader.domain.model.TocEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ResolveInitialLocatorUseCaseTest {

    private val useCase = ResolveInitialLocatorUseCase()

    @Test
    fun `returns saved locator when progress exists`() {
        val savedLocator = BookLocator(
            href = "chapter-2.xhtml",
            progressionInResource = 0.4,
            progressInBook = 0.3
        )
        val progress = ReadingProgress(
            bookId = BookId("book-1"),
            locator = savedLocator,
            updatedAtEpochMillis = 1L
        )

        val result = useCase(bookWithSpine(), progress)

        assertEquals(savedLocator, result)
    }

    @Test
    fun `returns first linear spine item when no saved progress`() {
        val book = bookWithSpine(
            SpineItem("s1", "cover.xhtml", "application/xhtml+xml", linear = false),
            SpineItem("s2", "chapter-1.xhtml", "application/xhtml+xml", linear = true),
            SpineItem("s3", "chapter-2.xhtml", "application/xhtml+xml", linear = true)
        )

        val result = useCase(book, savedProgress = null)

        assertEquals(
            BookLocator(
                href = "chapter-1.xhtml",
                progressionInResource = 0.0,
                progressInBook = 0.0
            ),
            result
        )
    }

    @Test
    fun `falls back to first spine item when no linear items`() {
        val book = bookWithSpine(
            SpineItem("s1", "preface.xhtml", "application/xhtml+xml", linear = false),
            SpineItem("s2", "chapter-1.xhtml", "application/xhtml+xml", linear = false)
        )

        val result = useCase(book, savedProgress = null)

        assertEquals(
            BookLocator(
                href = "preface.xhtml",
                progressionInResource = 0.0,
                progressInBook = 0.0
            ),
            result
        )
    }

    @Test
    fun `returns null when spine is empty`() {
        val book = bookWithSpine()

        val result = useCase(book, savedProgress = null)

        assertNull(result)
    }

    private fun bookWithSpine(vararg spine: SpineItem): EpubBook =
        EpubBook(
            id = BookId("book-1"),
            filePath = "/tmp/book.epub",
            extractedBasePath = "/tmp/book",
            title = "Test Book",
            language = "en",
            authors = listOf("Author"),
            coverImageHref = null,
            tableOfContents = listOf(TocEntry(null, "Chapter 1", "chapter-1.xhtml")),
            spine = spine.toList()
        )
}
