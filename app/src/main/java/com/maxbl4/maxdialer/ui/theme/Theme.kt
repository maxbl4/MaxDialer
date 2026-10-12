package com.maxbl4.maxdialer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Samsung One UI 8 palette: grey page, white rounded cards, blue accent. */
object OneUi {
    val callGreen = Color(0xFF1DAF55)
    val smsBlue = Color(0xFF3E91FF)
    val missedRed = Color(0xFFE5342B)
    val incomingBlue = Color(0xFF3E91FF)
    val outgoingGreen = Color(0xFF1DAF55)

    val cardRadius = 26.dp
    val screenPadding = 12.dp

    val avatarColors = listOf(
        Color(0xFF7E9CD8), Color(0xFF6FBF8E), Color(0xFFE39F74), Color(0xFFB096D6),
        Color(0xFFE08A9B), Color(0xFF5FB5BA), Color(0xFFD4B154),
    )
}

private val LightColors = lightColorScheme(
    primary = Color(0xFF0381FE),
    onPrimary = Color.White,
    background = Color(0xFFF2F2F2),
    onBackground = Color(0xFF000000),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF000000),
    onSurfaceVariant = Color(0xFF7A7A7A),
    surfaceContainerHigh = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFFFFFFF),
    outlineVariant = Color(0xFFE6E6E6),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF3E91FF),
    onPrimary = Color.White,
    background = Color(0xFF000000),
    onBackground = Color(0xFFFAFAFA),
    surface = Color(0xFF171717),
    onSurface = Color(0xFFFAFAFA),
    onSurfaceVariant = Color(0xFF8F8F8F),
    surfaceContainerHigh = Color(0xFF252525),
    surfaceContainer = Color(0xFF252525),
    outlineVariant = Color(0xFF2B2B2B),
)

@Composable
fun MaxDialerTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors, content = content)
}
