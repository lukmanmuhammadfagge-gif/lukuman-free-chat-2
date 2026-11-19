package com.lukuman.freechat.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lukuman.freechat.ui.theme.DeepBluePalette

/**
 * Original geometric deep-blue Lukuman Free Chat emblem.
 * Features interlocking cobalt and sapphire ribbons with an "L" beacon crest.
 */
@Composable
fun BrandMark(
    modifier: Modifier = Modifier,
    size: Dp = 76.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(DeepBluePalette.ElevatedSlateBlue, DeepBluePalette.DeepOcean),
                start = Offset.Zero,
                end = Offset(w, h)
            ),
            cornerRadius = CornerRadius(w * 0.28f, w * 0.28f),
            size = Size(w, h)
        )

        val outerRibbon = Path().apply {
            moveTo(w * 0.22f, h * 0.24f)
            lineTo(w * 0.62f, h * 0.24f)
            quadraticTo(w * 0.78f, h * 0.24f, w * 0.78f, h * 0.40f)
            lineTo(w * 0.78f, h * 0.56f)
            quadraticTo(w * 0.78f, h * 0.72f, w * 0.62f, h * 0.72f)
            lineTo(w * 0.42f, h * 0.72f)
            lineTo(w * 0.26f, h * 0.82f)
            lineTo(w * 0.30f, h * 0.68f)
            quadraticTo(w * 0.22f, h * 0.62f, w * 0.22f, h * 0.52f)
            close()
        }
        drawPath(
            path = outerRibbon,
            brush = Brush.linearGradient(
                colors = listOf(DeepBluePalette.RoyalCobalt, Color(0xFF2563EB))
            )
        )

        val innerBeacon = Path().apply {
            moveTo(w * 0.36f, h * 0.34f)
            lineTo(w * 0.68f, h * 0.34f)
            quadraticTo(w * 0.78f, h * 0.34f, w * 0.78f, h * 0.44f)
            lineTo(w * 0.78f, h * 0.52f)
            quadraticTo(w * 0.78f, h * 0.62f, w * 0.68f, h * 0.62f)
            lineTo(w * 0.46f, h * 0.62f)
            lineTo(w * 0.36f, h * 0.70f)
            close()
        }
        drawPath(path = innerBeacon, color = DeepBluePalette.SapphireSky)

        // Geometric "L" mark inside the sapphire ribbon
        val lMark = Path().apply {
            moveTo(w * 0.44f, h * 0.41f)
            lineTo(w * 0.50f, h * 0.41f)
            lineTo(w * 0.50f, h * 0.52f)
            lineTo(w * 0.64f, h * 0.52f)
            lineTo(w * 0.64f, h * 0.57f)
            lineTo(w * 0.44f, h * 0.57f)
            close()
        }
        drawPath(path = lMark, color = DeepBluePalette.IceInk)
    }
}
