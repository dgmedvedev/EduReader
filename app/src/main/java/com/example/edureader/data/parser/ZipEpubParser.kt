package com.example.edureader.data.parser

import android.content.Context
import com.example.edureader.domain.common.DomainError
import com.example.edureader.domain.common.DomainResult
import com.example.edureader.domain.model.BookId
import com.example.edureader.domain.model.EpubBook
import com.example.edureader.domain.model.SpineItem
import com.example.edureader.domain.model.TocEntry
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.util.zip.ZipFile
import javax.inject.Inject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

class ZipEpubParser @Inject constructor(
    @param:ApplicationContext private val context: Context
) : EpubParser {
    override suspend fun parse(filePath: String): DomainResult<EpubBook> =
        withContext(Dispatchers.IO) {
            try {
                val file = File(filePath)
                if (!file.exists() || !file.isFile) {
                    return@withContext DomainResult.Failure(
                        DomainError.NotFound("EPUB file does not exist at path: $filePath")
                    )
                }

                ZipFile(file).use { zip ->
                    val extractedBasePath = extractZipIfNeeded(file, zip)

                    val containerEntry = zip.getEntry(CONTAINER_XML)
                        ?: return@withContext DomainResult.Failure(
                            DomainError.Parsing("META-INF/container.xml is missing in EPUB.")
                        )

                    val opfPath = zip.getInputStream(containerEntry).use { parseContainerXml(it) }
                        ?: return@withContext DomainResult.Failure(
                            DomainError.Parsing("Unable to locate OPF package path in container.xml.")
                        )

                    val opfEntry = zip.getEntry(opfPath)
                        ?: return@withContext DomainResult.Failure(
                            DomainError.Parsing("OPF package is missing in EPUB: $opfPath")
                        )

                    val packageData = zip.getInputStream(opfEntry).use { parsePackageDocument(it) }
                    val opfBasePath = opfPath.substringBeforeLast('/', "")

                    val spine = packageData.spineItems.mapNotNull { itemRef ->
                        val manifestItem =
                            packageData.manifest[itemRef.idRef] ?: return@mapNotNull null
                        SpineItem(
                            idRef = itemRef.idRef,
                            href = resolveRelativePath(opfBasePath, manifestItem.href),
                            mediaType = manifestItem.mediaType,
                            linear = itemRef.linear
                        )
                    }

                    val toc = buildToc(
                        packageData = packageData,
                        opfBasePath = opfBasePath,
                        zip = zip
                    )
                    val cover = resolveCoverHref(packageData, opfBasePath)

                    DomainResult.Success(
                        EpubBook(
                            id = BookId(file.absolutePath),
                            filePath = file.absolutePath,
                            extractedBasePath = extractedBasePath,
                            title = packageData.title.ifBlank { file.nameWithoutExtension },
                            language = packageData.language,
                            authors = packageData.authors,
                            coverImageHref = cover,
                            tableOfContents = toc,
                            spine = spine
                        )
                    )
                }
            } catch (error: Exception) {
                DomainResult.Failure(
                    DomainError.Parsing(
                        message = "Failed to parse EPUB file.",
                        cause = error
                    )
                )
            }
        }

    private fun parseContainerXml(input: InputStream): String? {
        val parser = createXmlParser(input)
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG && parser.name == "rootfile") {
                return parser.getAttributeValue(null, "full-path")
            }
        }
        return null
    }

    private fun parsePackageDocument(input: InputStream): PackageData {
        val parser = createXmlParser(input)

        val manifest = linkedMapOf<String, ManifestItem>()
        val spineItems = mutableListOf<SpineRef>()
        val authors = mutableListOf<String>()
        var title = ""
        var language: String? = null
        var coverItemId: String? = null

        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType != XmlPullParser.START_TAG) continue
            when (parser.name) {
                "title" -> title = parser.nextText().trim()
                "language" -> language = parser.nextText().trim().ifBlank { null }
                "creator" -> {
                    val author = parser.nextText().trim()
                    if (author.isNotEmpty()) authors += author
                }

                "meta" -> {
                    val name = parser.getAttributeValue(null, "name")
                    if (name == "cover") {
                        coverItemId = parser.getAttributeValue(null, "content")
                    }
                }

                "item" -> {
                    val id = parser.getAttributeValue(null, "id") ?: continue
                    val href = parser.getAttributeValue(null, "href") ?: continue
                    val mediaType = parser.getAttributeValue(null, "media-type") ?: ""
                    val properties = parser.getAttributeValue(null, "properties")
                    manifest[id] = ManifestItem(id, href, mediaType, properties)
                }

                "itemref" -> {
                    val idRef = parser.getAttributeValue(null, "idref") ?: continue
                    val linear = parser.getAttributeValue(null, "linear") != "no"
                    spineItems += SpineRef(idRef, linear)
                }
            }
        }

        return PackageData(
            title = title,
            language = language,
            authors = authors,
            coverItemId = coverItemId,
            manifest = manifest,
            spineItems = spineItems
        )
    }

    private fun buildToc(
        packageData: PackageData,
        opfBasePath: String,
        zip: ZipFile
    ): List<TocEntry> {
        val navItems = packageData.manifest.values.filter { item ->
            item.properties?.split(" ")?.contains("nav") == true
        }

        if (navItems.isNotEmpty()) {
            navItems.forEach { item ->
                val navPath = resolveRelativePath(opfBasePath, item.href)
                val navEntry = zip.getEntry(navPath) ?: return@forEach
                val parsed = zip.getInputStream(navEntry).use { parseNavDocument(it, opfBasePath) }
                if (parsed.isNotEmpty()) return parsed
            }
        }

        return packageData.spineItems.mapNotNull { itemRef ->
            val manifestItem = packageData.manifest[itemRef.idRef] ?: return@mapNotNull null
            TocEntry(
                id = manifestItem.id,
                title = manifestItem.href.substringAfterLast('/').substringBeforeLast('.'),
                href = resolveRelativePath(opfBasePath, manifestItem.href)
            )
        }
    }

    private fun parseNavDocument(input: InputStream, opfBasePath: String): List<TocEntry> {
        val html = input.bufferedReader().use { it.readText() }
        val linkRegex = Regex(
            pattern = """<a\b[^>]*href\s*=\s*["']([^"']+)["'][^>]*>(.*?)</a>""",
            options = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
        val tagRegex = Regex("""<[^>]+>""")
        val whitespaceRegex = Regex("""\s+""")

        return linkRegex.findAll(html)
            .mapIndexedNotNull { index, match ->
                val href = match.groupValues[1].trim()
                val rawTitle = match.groupValues[2]
                val title = rawTitle
                    .replace(tagRegex, " ")
                    .replace(whitespaceRegex, " ")
                    .trim()
                if (href.isBlank() || title.isBlank()) {
                    null
                } else {
                    TocEntry(
                        id = "nav-$index",
                        title = title,
                        href = resolveRelativePath(opfBasePath, href.substringBefore('#'))
                    )
                }
            }
            .toList()
    }

    private fun resolveCoverHref(packageData: PackageData, opfBasePath: String): String? {
        val directCover = packageData.coverItemId?.let { packageData.manifest[it] }
        if (directCover != null) return resolveRelativePath(opfBasePath, directCover.href)

        val coverByProperty = packageData.manifest.values.firstOrNull { item ->
            item.properties?.split(" ")?.contains("cover-image") == true
        }
        return coverByProperty?.let { resolveRelativePath(opfBasePath, it.href) }
    }

    private fun resolveRelativePath(basePath: String, relativePath: String): String {
        if (basePath.isBlank()) return relativePath
        return "$basePath/$relativePath"
    }

    private fun createXmlParser(input: InputStream): XmlPullParser {
        return XmlPullParserFactory.newInstance().newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            setInput(input, null)
        }
    }

    private fun extractZipIfNeeded(file: File, zip: ZipFile): String {
        val targetDir = File(context.filesDir, "epub_extracted/${hash(file.absolutePath)}")
        val marker = File(targetDir, ".extracted")
        if (marker.exists()) return targetDir.absolutePath

        targetDir.mkdirs()
        val baseCanonical = targetDir.canonicalPath + File.separator
        val entries = zip.entries()
        while (entries.hasMoreElements()) {
            val entry = entries.nextElement()
            val outFile = File(targetDir, entry.name)

            if (entry.isDirectory) {
                outFile.mkdirs()
                continue
            }

            val outCanonical = outFile.canonicalPath
            require(outCanonical.startsWith(baseCanonical)) {
                "Zip slip detected for entry: ${entry.name}"
            }

            outFile.parentFile?.mkdirs()
            zip.getInputStream(entry).use { input ->
                FileOutputStream(outFile).use { output ->
                    input.copyTo(output)
                }
            }
        }
        marker.writeText("ok")
        return targetDir.absolutePath
    }

    private fun hash(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }.take(24)
    }

    private data class PackageData(
        val title: String,
        val language: String?,
        val authors: List<String>,
        val coverItemId: String?,
        val manifest: Map<String, ManifestItem>,
        val spineItems: List<SpineRef>
    )

    private data class ManifestItem(
        val id: String,
        val href: String,
        val mediaType: String,
        val properties: String?
    )

    private data class SpineRef(
        val idRef: String,
        val linear: Boolean
    )

    private companion object {
        const val CONTAINER_XML = "META-INF/container.xml"
    }
}
