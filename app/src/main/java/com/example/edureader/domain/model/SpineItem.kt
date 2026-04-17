package com.example.edureader.domain.model

data class SpineItem(
    val idRef: String,
    val href: String,
    val mediaType: String,
    val linear: Boolean = true
)
