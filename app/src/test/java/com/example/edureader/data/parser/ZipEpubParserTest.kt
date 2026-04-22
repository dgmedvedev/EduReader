package com.example.edureader.data.parser

import com.example.edureader.domain.common.DomainError
import com.example.edureader.domain.common.DomainResult
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.createTempDirectory
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class ZipEpubParserTest {

    @Test
    fun `parse returns parsing error for invalid mimetype`() = runBlocking {
        val workspace = createTempDirectory("zip-parser-invalid").toFile()
        val epubFile = File(workspace, "invalid.epub")
        createEpubArchive(epubFile, mimeType = "text/plain")
        val parser = ZipEpubParser(FakeEpubStorage(workspace))

        val result = parser.parse(epubFile.absolutePath)

        assertTrue(result is DomainResult.Failure)
        assertTrue((result as DomainResult.Failure).error is DomainError.Parsing)
    }

    @Test
    fun `parse returns parsing error when container is missing`() = runBlocking {
        val workspace = createTempDirectory("zip-parser-valid").toFile()
        val epubFile = File(workspace, "missing-container.epub")
        createCorruptedArchiveWithoutContainer(epubFile)
        val parser = ZipEpubParser(FakeEpubStorage(workspace))

        val result = parser.parse(epubFile.absolutePath)

        assertTrue(result is DomainResult.Failure)
        assertTrue((result as DomainResult.Failure).error is DomainError.Parsing)
    }

    private fun createEpubArchive(output: File, mimeType: String) {
        ZipOutputStream(output.outputStream().buffered()).use { zip ->
            zip.putNextEntry(ZipEntry("mimetype"))
            zip.write(mimeType.toByteArray())
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("META-INF/container.xml"))
            zip.write(
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
                  <rootfiles>
                    <rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/>
                  </rootfiles>
                </container>
                """.trimIndent().toByteArray()
            )
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("OEBPS/content.opf"))
            zip.write(
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <package xmlns="http://www.idpf.org/2007/opf" version="3.0" unique-identifier="book-id">
                  <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
                    <dc:title>Test Book</dc:title>
                    <dc:creator>Test Author</dc:creator>
                    <dc:language>en</dc:language>
                  </metadata>
                  <manifest>
                    <item id="nav" href="nav.xhtml" media-type="application/xhtml+xml" properties="nav"/>
                    <item id="c1" href="chapter-1.xhtml" media-type="application/xhtml+xml"/>
                    <item id="c2" href="chapter-2.xhtml" media-type="application/xhtml+xml"/>
                  </manifest>
                  <spine>
                    <itemref idref="c1"/>
                    <itemref idref="c2"/>
                  </spine>
                </package>
                """.trimIndent().toByteArray()
            )
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("OEBPS/nav.xhtml"))
            zip.write(
                """
                <html xmlns="http://www.w3.org/1999/xhtml">
                  <head><title>TOC</title></head>
                  <body>
                    <nav xmlns:epub="http://www.idpf.org/2007/ops" epub:type="toc">
                      <ol>
                        <li><a href="chapter-1.xhtml#intro">Chapter One</a></li>
                        <li><a href="chapter-2.xhtml">Chapter Two</a></li>
                      </ol>
                    </nav>
                  </body>
                </html>
                """.trimIndent().toByteArray()
            )
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("OEBPS/chapter-1.xhtml"))
            zip.write("<html><body>Chapter 1</body></html>".toByteArray())
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("OEBPS/chapter-2.xhtml"))
            zip.write("<html><body>Chapter 2</body></html>".toByteArray())
            zip.closeEntry()
        }
    }

    private fun createCorruptedArchiveWithoutContainer(output: File) {
        ZipOutputStream(output.outputStream().buffered()).use { zip ->
            zip.putNextEntry(ZipEntry("mimetype"))
            zip.write("application/epub+zip".toByteArray())
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("OEBPS/content.opf"))
            zip.write(
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <package xmlns="http://www.idpf.org/2007/opf" version="3.0">
                  <manifest>
                    <item id="c1" href="chapter-1.xhtml" media-type="application/xhtml+xml"/>
                  </manifest>
                  <spine>
                    <itemref idref="c1"/>
                  </spine>
                </package>
                """.trimIndent().toByteArray()
            )
            zip.closeEntry()
        }
    }

    private class FakeEpubStorage(
        private val rootDir: File
    ) : EpubStorage {
        override fun getExtractionDir(file: File): File {
            return File(rootDir, file.nameWithoutExtension)
        }
    }
}
