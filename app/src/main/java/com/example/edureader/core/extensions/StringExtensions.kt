package com.example.edureader.core.extensions

import com.example.edureader.presentation.reader.contract.TextSpec

internal fun String.toTextSpecOrNull(): TextSpec? {
    val clean = this.trim()
    return clean.takeIf { it.isNotBlank() }?.let { TextSpec.Raw(it) }
}
