package com.example.edureader.data.parser

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject

class AndroidEpubStorage @Inject constructor(
    @param:ApplicationContext private val context: Context
) : EpubStorage {

    override fun getExtractionDir(file: File): File {
        return File(context.filesDir, "epub_extracted/${hash(file.absolutePath)}")
    }

    private fun hash(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }.take(24)
    }
}
