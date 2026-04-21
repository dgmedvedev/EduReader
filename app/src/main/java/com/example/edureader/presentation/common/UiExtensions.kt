package com.example.edureader.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.edureader.presentation.reader.contract.TextSpec

@Composable
fun TextSpec.asString(): String =
    when (this) {
        is TextSpec.Raw -> value
        is TextSpec.Res -> stringResource(id, *args.toTypedArray())
    }
