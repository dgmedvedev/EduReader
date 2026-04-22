package com.example.edureader.domain.common

sealed interface DomainError {
    data class Validation(override val message: String) : DomainError
    data class NotFound(override val message: String) : DomainError
    data class Storage(override val message: String, val cause: Throwable? = null) : DomainError
    data class Parsing(override val message: String, val cause: Throwable? = null) : DomainError
    data class Unknown(override val message: String, val cause: Throwable? = null) : DomainError

    val message: String
}
