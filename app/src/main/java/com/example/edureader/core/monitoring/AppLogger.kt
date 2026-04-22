package com.example.edureader.core.monitoring

import com.example.edureader.domain.common.DomainError

interface AppLogger {
    fun d(tag: String, message: String)
    fun i(tag: String, message: String)
    fun w(tag: String, message: String, throwable: Throwable? = null)
    fun e(tag: String, message: String, throwable: Throwable? = null)
    fun reportDomainError(source: String, error: DomainError)
}
