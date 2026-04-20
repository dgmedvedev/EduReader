package com.example.edureader.data.local

import androidx.core.content.edit
import javax.inject.Inject
import javax.inject.Singleton

interface BookCatalogLocalDataSource {
    fun saveBookPath(bookId: String, filePath: String)
    fun getBookPath(bookId: String): String?
}

@Singleton
class SharedPrefsBookCatalogLocalDataSource @Inject constructor(
    private val preferences: android.content.SharedPreferences
) : BookCatalogLocalDataSource {

    override fun saveBookPath(bookId: String, filePath: String) {
        preferences.edit { putString(key(bookId), filePath) }
    }

    override fun getBookPath(bookId: String): String? {
        return preferences.getString(key(bookId), null)
    }

    private fun key(bookId: String): String = "book_path_$bookId"
}
