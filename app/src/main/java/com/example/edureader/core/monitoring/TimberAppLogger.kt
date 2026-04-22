package com.example.edureader.core.monitoring

import com.example.edureader.domain.common.DomainError
import com.google.firebase.crashlytics.FirebaseCrashlytics
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

@Singleton
class TimberAppLogger @Inject constructor() : AppLogger {

    override fun d(tag: String, message: String) {
        Timber.tag(tag).d(message)
    }

    override fun i(tag: String, message: String) {
        Timber.tag(tag).i(message)
    }

    override fun w(tag: String, message: String, throwable: Throwable?) {
        if (throwable == null) {
            Timber.tag(tag).w(message)
        } else {
            Timber.tag(tag).w(throwable, message)
        }
    }

    override fun e(tag: String, message: String, throwable: Throwable?) {
        if (throwable == null) {
            Timber.tag(tag).e(message)
        } else {
            Timber.tag(tag).e(throwable, message)
        }
    }

    override fun reportDomainError(source: String, error: DomainError) {
        val cause = when (error) {
            is DomainError.Storage -> error.cause
            is DomainError.Parsing -> error.cause
            is DomainError.Unknown -> error.cause
            else -> null
        }
        val details = "source=$source, type=${error::class.simpleName}, message=${error.message}"
        if (cause == null) {
            Timber.tag(TAG).w(details)
            runCatching {
                FirebaseCrashlytics.getInstance().log(details)
            }
        } else {
            Timber.tag(TAG).e(cause, details)
            runCatching {
                FirebaseCrashlytics.getInstance().recordException(cause)
            }
        }
    }

    private companion object {
        private const val TAG = "DomainError"
    }
}
