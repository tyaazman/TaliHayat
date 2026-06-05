package com.group.talihayat.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = Teal,
    onPrimary = Surface,
    primaryContainer = TealLight,
    onPrimaryContainer = TealDark,
    secondary = Navy,
    onSecondary = Surface,
    error = Error,
    background = Background,
    onBackground = PrimaryText,
    surface = Surface,
    onSurface = PrimaryText
)

@Composable
fun TaliHayatTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
