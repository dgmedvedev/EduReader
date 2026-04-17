package com.example.edureader.domain.model

@JvmInline
value class BookId(val value: String) {
    init {
        require(value.isNotBlank()) { "BookId must not be blank" }
    }
}
