package com.example.edureader.data.local

import android.content.SharedPreferences
import com.example.edureader.domain.model.BookId
import com.example.edureader.domain.model.BookLocator
import com.example.edureader.domain.model.ReadingProgress
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.json.JSONObject
import androidx.core.content.edit

interface ReadingProgressLocalDataSource {
    fun get(bookId: BookId): ReadingProgress?
    fun observe(bookId: BookId): Flow<ReadingProgress?>
    fun save(progress: ReadingProgress)
    fun getLastOpenedBookId(): BookId?
}

@Singleton
class SharedPrefsReadingProgressLocalDataSource @Inject constructor(
    private val preferences: SharedPreferences
) : ReadingProgressLocalDataSource {

    override fun get(bookId: BookId): ReadingProgress? {
        val raw = preferences.getString(key(bookId), null) ?: return null
        return parse(raw, bookId)
    }

    override fun observe(bookId: BookId): Flow<ReadingProgress?> = callbackFlow {
        trySend(get(bookId))

        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changedKey ->
            if (changedKey == key(bookId)) {
                trySend(get(bookId))
            }
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)

        awaitClose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    override fun save(progress: ReadingProgress) {
        val json = JSONObject()
            .put("href", progress.locator.href)
            .put("progressionInResource", progress.locator.progressionInResource)
            .put("progressInBook", progress.locator.progressInBook)
            .put("updatedAtEpochMillis", progress.updatedAtEpochMillis)
            .toString()

        preferences.edit {
            putString(key(progress.bookId), json)
            putString(KEY_LAST_OPENED_BOOK_ID, progress.bookId.value)
        }
    }

    override fun getLastOpenedBookId(): BookId? {
        val rawBookId = preferences.getString(KEY_LAST_OPENED_BOOK_ID, null) ?: return null
        return runCatching { BookId(rawBookId) }.getOrNull()
    }

    private fun parse(raw: String, bookId: BookId): ReadingProgress? {
        return try {
            val json = JSONObject(raw)
            ReadingProgress(
                bookId = bookId,
                locator = BookLocator(
                    href = json.getString("href"),
                    progressionInResource = json.getDouble("progressionInResource"),
                    progressInBook = json.getDouble("progressInBook")
                ),
                updatedAtEpochMillis = json.getLong("updatedAtEpochMillis")
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun key(bookId: BookId): String = "reading_progress_${bookId.value}"

    private companion object {
        const val KEY_LAST_OPENED_BOOK_ID = "last_opened_book_id"
    }
}
