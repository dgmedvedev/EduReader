package com.example.edureader.presentation.reader.contract

import androidx.annotation.StringRes

sealed interface TextSpec {
    data class Res(
        @param:StringRes val id: Int,
        val args: List<Any> = emptyList()
    ) : TextSpec

    data class Raw(val value: String) : TextSpec
}
