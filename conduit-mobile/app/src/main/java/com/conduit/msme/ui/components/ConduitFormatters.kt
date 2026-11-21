package com.conduit.msme.ui.components

import kotlin.math.roundToLong

/**
 * Formats an amount using standard Indian numbering system grouping
 * (e.g. 50000 -> "50,000", 186000 -> "1,86,000").
 */
fun formatInrNumber(amount: Double): String {
    val whole = amount.roundToLong()
    val negative = whole < 0
    val digits = kotlin.math.abs(whole).toString()
    if (digits.length <= 3) {
        return if (negative) "-$digits" else digits
    }
    val lastThree = digits.takeLast(3)
    val remaining = digits.dropLast(3)
    val groupedPrefix = remaining.reversed()
        .chunked(2)
        .joinToString(",")
        .reversed()
    val formatted = "$groupedPrefix,$lastThree"
    return if (negative) "-$formatted" else formatted
}
