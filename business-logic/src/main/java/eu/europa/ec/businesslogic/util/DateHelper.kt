/*
 * Copyright (c) 2023 European Commission
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European
 * Commission - subsequent versions of the EUPL (the "Licence"); You may not use this work
 * except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the Licence is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific language
 * governing permissions and limitations under the Licence.
 */

package eu.europa.ec.businesslogic.util

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Date
import java.util.Locale

private val dtoDateFormatters = listOf(
    "yyyy-MM-dd'T'HH:mm:ss.SSS",
    "yyyy-MM-dd'T'HH:mm:ss",
    "yyyy-MM-dd'T'HH:mm:ss.SSSz",
    "yyyy-MM-dd'T'HH:mm:ssz",
    "yyyy-MM-dd"
)

fun String.toDateFormatted(
    selectedLanguage: String = LocaleUtils.DEFAULT_LOCALE,
    dateFormatStyle: Int = DateFormat.MEDIUM,
): String? {
    var formattedDate: Date? = null
    val dateFormat = SimpleDateFormat.getDateInstance(
        dateFormatStyle,
        LocaleUtils.getLocaleFromSelectedLanguage(selectedLanguage)
    )
    for (formatter in dtoDateFormatters) {
        try {
            formattedDate = SimpleDateFormat(formatter, Locale.ENGLISH).parse(this)
            break
        } catch (_: Exception) {
            continue
        }
    }
    return formattedDate?.let { dateFormat.format(it) }
}

fun LocalDate.toDateFormatted(
    selectedLanguage: String = LocaleUtils.DEFAULT_LOCALE,
    dateFormatStyle: Int = DateFormat.MEDIUM
): String? {
    return try {
        val locale = LocaleUtils.getLocaleFromSelectedLanguage(selectedLanguage)
        val dateFormat = DateFormat.getDateInstance(dateFormatStyle, locale)
        dateFormat.format(Date.from(this.atStartOfDay(ZoneId.systemDefault()).toInstant()))
    } catch (e: Exception) {
        null
    }
}

fun String.toLocalDate(
    selectedLanguage: String = LocaleUtils.DEFAULT_LOCALE,
): LocalDate? {
    var dateFormatter: DateTimeFormatter?
    var result: LocalDate? = null

    for (formatter in dtoDateFormatters) {
        try {
            dateFormatter = DateTimeFormatter.ofPattern(
                formatter,
                LocaleUtils.getLocaleFromSelectedLanguage(selectedLanguage)
            )
            result = LocalDate.parse(this, dateFormatter)
            break
        } catch (_: Exception) {
            continue
        }
    }

    return result
}

fun Long.toLocalDate(): LocalDate {
    val instant = Instant.ofEpochMilli(this)
    return instant.atZone(ZoneId.systemDefault()).toLocalDate()
}

fun Instant.formatInstant(
    zoneId: ZoneId = ZoneId.systemDefault(),
    locale: Locale = Locale.ENGLISH
): String {
    val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy", locale)
        .withZone(zoneId)
    return formatter.format(this)
}

fun LocalDateTime.isWithinLastHour(): Boolean {
    return ChronoUnit.MINUTES.between(this, LocalDateTime.now()) < 60
}

fun LocalDateTime.minutesToNow(): Long {
    return ChronoUnit.MINUTES.between(this, LocalDateTime.now())
}

fun LocalDateTime.isToday(): Boolean {
    return this.toLocalDate() == LocalDateTime.now().toLocalDate()
}

fun LocalDateTime.isWithinThisWeek(): Boolean {
    val startOfWeek = LocalDateTime.now().toLocalDate()
        .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val endOfWeek = startOfWeek.plusDays(6)

    return this.toLocalDate().isAfter(startOfWeek.minusDays(1)) &&
            this.toLocalDate().isBefore(endOfWeek.plusDays(1))
}

fun LocalDateTime.startOfDay(): LocalDateTime =
    this.withHour(0).withMinute(0).withSecond(0)

fun LocalDateTime.endOfDay(): LocalDateTime =
    this.withHour(23).withMinute(59).withSecond(59)

fun LocalDateTime.startOfWeek(): LocalDateTime =
    this.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).startOfDay()

fun LocalDateTime.endOfWeek(): LocalDateTime =
    this.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)).endOfDay()

fun LocalDateTime.startOfMonth(): LocalDateTime =
    this.with(TemporalAdjusters.firstDayOfMonth()).startOfDay()

fun LocalDateTime.endOfMonth(): LocalDateTime =
    this.with(TemporalAdjusters.lastDayOfMonth()).endOfDay()

fun String.toInstantOrNull(
    pattern: String = FULL_DATETIME_PATTERN,
    locale: Locale = Locale.getDefault()
): Instant? = runCatching {
    val formatter = DateTimeFormatter.ofPattern(pattern, locale)
    LocalDateTime.parse(this, formatter)
        .atZone(ZoneId.systemDefault())
        .toInstant()
}.getOrNull()

fun Instant.plusOneDay(): Instant {
    return if (this == Instant.MAX) {
        this
    } else {
        this.atZone(ZoneId.systemDefault())
            .toLocalDate()
            .plusDays(1)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
    }
}

fun convertMillisToDate(millis: Long): String {
    val formatter = SimpleDateFormat(DAY_MONTH_YEAR_TEXT_FIELD_PATTERN, Locale.getDefault())
    return formatter.format(Date(millis))
}

fun Long?.toDisplayedDate(): String {
    return this?.let { dateValue ->
        convertMillisToDate(dateValue).takeIf {
            dateValue > Long.MIN_VALUE && dateValue < Long.MAX_VALUE
        }
    }.orEmpty()
}

private const val FULL_DATETIME_PATTERN = "dd MMM yyyy hh:mm a"
private const val HOURS_MINUTES_DATETIME_PATTERN = "hh:mm a"
private const val MONTH_YEAR_DATETIME_PATTERN = "MMMM yyyy"
private const val DAY_MONTH_YEAR_TEXT_FIELD_PATTERN = "dd/MM/yyyy"

val fullDateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern(FULL_DATETIME_PATTERN)
val hoursMinutesFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern(HOURS_MINUTES_DATETIME_PATTERN)
val monthYearFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern(MONTH_YEAR_DATETIME_PATTERN)
