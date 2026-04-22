package com.example.edureader.presentation.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = ReaderPrimaryDark,
    onPrimary = ReaderOnPrimaryDark,
    primaryContainer = ReaderPrimaryContainerDark,
    onPrimaryContainer = ReaderOnPrimaryContainerDark,
    secondary = ReaderSecondaryDark,
    onSecondary = ReaderOnSecondaryDark,
    secondaryContainer = ReaderSecondaryContainerDark,
    onSecondaryContainer = ReaderOnSecondaryContainerDark,
    background = ReaderBackgroundDark,
    surface = ReaderSurfaceDark,
    onSurface = ReaderOnSurfaceDark,
    onSurfaceVariant = ReaderOnSurfaceVariantDark,
    outline = ReaderOutlineDark
)

private val LightColorScheme = lightColorScheme(
    primary = ReaderPrimaryLight,
    onPrimary = ReaderOnPrimaryLight,
    primaryContainer = ReaderPrimaryContainerLight,
    onPrimaryContainer = ReaderOnPrimaryContainerLight,
    secondary = ReaderSecondaryLight,
    onSecondary = ReaderOnSecondaryLight,
    secondaryContainer = ReaderSecondaryContainerLight,
    onSecondaryContainer = ReaderOnSecondaryContainerLight,
    background = ReaderBackgroundLight,
    surface = ReaderSurfaceLight,
    onSurface = ReaderOnSurfaceLight,
    onSurfaceVariant = ReaderOnSurfaceVariantLight,
    outline = ReaderOutlineLight
)

@Composable
internal fun EduReaderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}