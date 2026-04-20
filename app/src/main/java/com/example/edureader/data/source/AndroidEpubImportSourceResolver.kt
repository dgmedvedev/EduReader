package com.example.edureader.data.source

import android.content.Context
import androidx.core.net.toUri
import com.example.edureader.domain.common.DomainError
import com.example.edureader.domain.common.DomainResult
import com.example.edureader.domain.service.EpubImportSourceResolver
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class AndroidEpubImportSourceResolver @Inject constructor(
    @param:ApplicationContext private val context: Context
) : EpubImportSourceResolver {
    override suspend fun copyUriToLocalFile(uriString: String): DomainResult<String> =
        withContext(Dispatchers.IO) {
            try {
                val uri = uriString.toUri()

                val targetDir = File(context.filesDir, "imported_epub").apply { mkdirs() }
                val target = File(targetDir, "book_${System.currentTimeMillis()}.epub")

                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(target).use { output ->
                        input.copyTo(output)
                    }
                } ?: return@withContext DomainResult.Failure(
                    DomainError.Storage(
                        message = "Unable to open input stream for URI: $uriString",
                        cause = null
                    )
                )

                DomainResult.Success(target.absolutePath)

            } catch (e: Exception) {
                DomainResult.Failure(
                    DomainError.Storage(
                        message = "Failed to copy EPUB from selected URI.",
                        cause = e
                    )
                )
            }
        }
}
