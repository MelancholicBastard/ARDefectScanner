package com.example.ardefectscanner.ui.theme

import androidx.compose.foundation.shape.GenericShape
import kotlin.math.cos
import kotlin.math.sin

val Cookie = GenericShape { size, _ ->
    val radius = minOf(size.width, size.height) / 2f
    for (step in 0..240) {
        val angle = step * 2.0 * Math.PI / 240
        val r = radius * (0.91 + 0.08 * cos(9 * angle))
        val x = size.width / 2 + (r * cos(angle)).toFloat()
        val y = size.height / 2 + (r * sin(angle)).toFloat()
        if (step == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

