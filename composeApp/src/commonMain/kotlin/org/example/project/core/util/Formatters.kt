// commonMain/kotlin/org/example/project/core/util/Formatters.kt
package org.example.project.core.util

import kotlin.math.abs
import kotlin.math.roundToLong

fun fmt2(value: Double): String = fmtFixed(value, 2)
fun fmt1(value: Double): String = fmtFixed(value, 1)

fun fmtFixed(value: Double, decimals: Int): String {
    if (value.isNaN() || value.isInfinite()) return "--"

    val sign = if (value < 0) "-" else ""
    val absValue = abs(value)

    val pow10 = pow10(decimals)
    val scaled = (absValue * pow10.toDouble()).roundToLong()

    val intPart = scaled / pow10
    val fracPart = (scaled % pow10).toInt()

    return if (decimals == 0) {
        "$sign$intPart"
    } else {
        "$sign$intPart.${fracPart.toString().padStart(decimals, '0')}"
    }
}

private fun pow10(decimals: Int): Long {
    var p = 1L
    repeat(decimals.coerceAtLeast(0)) { p *= 10L }
    return p
}
