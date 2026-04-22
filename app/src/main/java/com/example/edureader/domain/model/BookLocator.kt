package com.example.edureader.domain.model

/**
 * Stable locator inside EPUB content.
 * [progressInBook] is normalized in 0.0..1.0 range.
 */
data class BookLocator(
    val href: String,
    val progressionInResource: Double,
    val progressInBook: Double
) {
    init {
        require(href.isNotBlank()) { "href must not be blank" }
        require(progressionInResource in 0.0..1.0) { "progressionInResource must be in 0.0..1.0" }
        require(progressInBook in 0.0..1.0) { "progressInBook must be in 0.0..1.0" }
    }
}
