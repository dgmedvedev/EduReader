package com.example.edureader.presentation.common

import com.example.edureader.R
import com.example.edureader.domain.common.DomainError
import com.example.edureader.presentation.reader.contract.TextSpec

fun DomainError.toTextSpec(): TextSpec =
    when (this) {
        is DomainError.Validation -> TextSpec.Res(R.string.reader_error_validation)
        is DomainError.NotFound -> TextSpec.Res(R.string.reader_error_not_found)
        is DomainError.Storage -> TextSpec.Res(R.string.reader_error_storage)
        is DomainError.Parsing -> TextSpec.Res(R.string.reader_error_parsing)
        is DomainError.Unknown -> TextSpec.Res(R.string.reader_error_generic)
    }
