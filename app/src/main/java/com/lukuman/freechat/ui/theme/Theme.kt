package com.lukuman.freechat.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object DeepBluePalette {
    val MidnightNavy = Color(0xFF071126)
    val DeepOcean = Color(0xFF0C1B3B)
    val AbyssSurface = Color(0xFF122650)
    val ElevatedSlateBlue = Color(0xFF173166)
    val RoyalCobalt = Color(0xFF1D4ED8)
    val SapphireSky = Color(0xFF38BDF8)
    val IceInk = Color(0xFFF0F6FF)
    val SteelMuted = Color(0xFF96ACD0)
    val HairlineBorder = Color(0xFF213C73)
    val OutgoingBubble = Color(0xFF1E40AF)
    val IncomingBubble = Color(0xFF132954)
    val ErrorRed = Color(0xFFF87171)
}

private val LukumanColorScheme = darkColorScheme(
    primary = DeepBluePalette.RoyalCobalt,
    onPrimary = DeepBluePalette.IceInk,
    primaryContainer = DeepBluePalette.ElevatedSlateBlue,
    onPrimaryContainer = DeepBluePalette.SapphireSky,
    secondary = DeepBluePalette.SapphireSky,
    onSecondary = DeepBluePalette.MidnightNavy,
    background = DeepBluePalette.MidnightNavy,
    onBackground = DeepBluePalette.IceInk,
    surface = DeepBluePalette.DeepOcean,
    onSurface = DeepBluePalette.IceInk,
    surfaceVariant = DeepBluePalette.AbyssSurface,
    onSurfaceVariant = DeepBluePalette.SteelMuted,
    outline = DeepBluePalette.HairlineBorder,
    error = DeepBluePalette.ErrorRed,
    onError = DeepBluePalette.MidnightNavy
)

private val LukumanTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.4).sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )
)

@Composable
fun LukumanFreeChatTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LukumanColorScheme,
        typography = LukumanTypography,
        content = content
    )
}
