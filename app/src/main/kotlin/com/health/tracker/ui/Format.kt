package com.health.tracker.ui

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private val dayFormatter: DateTimeFormatter = DateTimeFormatterBuilder()
    .appendValue(ChronoField.YEAR)
    .appendLiteral("-")
    .appendValue(ChronoField.MONTH_OF_YEAR, 2)
    .appendLiteral("-")
    .appendValue(ChronoField.DAY_OF_MONTH, 2)
    .toFormatter(Locale.ROOT)

fun Double.kcalText(): String = "${roundToInt()} kcal"

fun Double.kgText(): String = String.format(Locale.ROOT, "%.1f kg", this)

fun Double.kgNumberText(): String = String.format(Locale.ROOT, "%.1f", this)

fun Double.signedKgText(): String {
    val value = String.format(Locale.ROOT, "%.1f", abs(this))
    return when {
        this > 0.05 -> "+$value kg"
        this < -0.05 -> "-$value kg"
        else -> "0.0 kg"
    }
}

fun Double.percentText(): String = String.format(Locale.ROOT, "%.1f%%", this)

fun Double.bmiText(): String = String.format(Locale.ROOT, "%.1f", this)

fun LocalDate.displayText(): String = format(dayFormatter)
