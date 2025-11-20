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
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Date

private val dtoDateFormatters = listOf(
    "yyyy-MM-dd'T'HH:mm:ss.SSS",
    "yyyy-MM-dd'T'HH:mm:ss",
    "yyyy-MM-dd'T'HH:mm:ss.SSSz",
    "yyyy-MM-dd'T'HH:mm:ssz",
    "yyyy-MM-dd"
)

private const val DAY_MONTH_YEAR_SHORT_PATTERN = "dd MMM yyyy"
const val FULL_DATETIME_PATTERN = "dd MMM yyyy hh:mm a"
private const val HOURS_MINUTES_DATETIME_PATTERN = "hh:mm a"
private const val MONTH_YEAR_DATETIME_PATTERN = "MMMM yyyy"
private const val DAY_MONTH_YEAR_TEXT_FIELD_PATTERN = "dd/MM/yyyy"

val fullDateTimeFormatter: DateTimeFormatter = DateTimeFormatter
    .ofPattern(FULL_DATETIME_PATTERN)
    .withLocale(LocaleUtils.getLocaleFromSelectedLanguage(LocaleUtils.PROJECT_DEFAULT_LOCALE))

val hoursMinutesFormatter: DateTimeFormatter = DateTimeFormatter
    .ofPattern(HOURS_MINUTES_DATETIME_PATTERN)
    .withLocale(LocaleUtils.getLocaleFromSelectedLanguage(LocaleUtils.PROJECT_DEFAULT_LOCALE))

val monthYearFormatter: DateTimeFormatter = DateTimeFormatter
    .ofPattern(MONTH_YEAR_DATETIME_PATTERN)
    .withLocale(LocaleUtils.getLocaleFromSelectedLanguage(LocaleUtils.PROJECT_DEFAULT_LOCALE))

fun String.toDateFormatted(
    selectedLanguage: String = LocaleUtils.PROJECT_DEFAULT_LOCALE,
    dateFormatStyle: Int = DateFormat.MEDIUM,
): String? {
    var formattedDate: Date? = null
    val locale = LocaleUtils.getLocaleFromSelectedLanguage(selectedLanguage)
    val dateFormat = SimpleDateFormat.getDateInstance(
        dateFormatStyle,
        locale
    )
    for (formatter in dtoDateFormatters) {
        try {
            formattedDate = SimpleDateFormat(formatter, locale).parse(this)
            break
        } catch (_: Exception) {
            continue
        }
    }
    return formattedDate?.let { dateFormat.format(it) }
}

fun LocalDate.toDateFormatted(
    selectedLanguage: String = LocaleUtils.PROJECT_DEFAULT_LOCALE,
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

fun LocalDateTime.formatLocalDateTime(
    pattern: String = DAY_MONTH_YEAR_SHORT_PATTERN,
    selectedLanguage: String = LocaleUtils.PROJECT_DEFAULT_LOCALE,
): String {
    val formatter = DateTimeFormatter.ofPattern(
        pattern,
        LocaleUtils.getLocaleFromSelectedLanguage(selectedLanguage)
    )
    return this.format(formatter)
}


fun String.toLocalDate(
    selectedLanguage: String = LocaleUtils.PROJECT_DEFAULT_LOCALE,
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

fun Instant.formatInstant(
    pattern: String = DAY_MONTH_YEAR_SHORT_PATTERN,
    zoneId: ZoneId = ZoneId.systemDefault(),
    selectedLanguage: String = LocaleUtils.PROJECT_DEFAULT_LOCALE,
): String {
    val formatter = DateTimeFormatter.ofPattern(
        pattern,
        LocaleUtils.getLocaleFromSelectedLanguage(selectedLanguage)
    ).withZone(zoneId)
    return formatter.format(this)
}

fun LocalDateTime.isJustNow(): Boolean {
    return ChronoUnit.MINUTES.between(this, LocalDateTime.now()) == 0L
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
    selectedLanguage: String = LocaleUtils.PROJECT_DEFAULT_LOCALE,
): Instant? = runCatching {
    val formatter = DateTimeFormatter.ofPattern(
        pattern,
        LocaleUtils.getLocaleFromSelectedLanguage(selectedLanguage)
    )
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

fun convertMillisToDate(
    millis: Long,
    selectedLanguage: String = LocaleUtils.PROJECT_DEFAULT_LOCALE,
): String {
    val formatter = SimpleDateFormat(
        DAY_MONTH_YEAR_TEXT_FIELD_PATTERN,
        LocaleUtils.getLocaleFromSelectedLanguage(selectedLanguage)
    )
    return formatter.format(Date(millis))
}

fun Long?.toDisplayedDate(): String {
    return this?.let { dateValue ->
        convertMillisToDate(dateValue).takeIf {
            dateValue > Long.MIN_VALUE && dateValue < Long.MAX_VALUE
        }
    }.orEmpty()
}

fun convertLocalDateToFormattedString(
    localDate: LocalDate,
    selectedLanguage: String = LocaleUtils.PROJECT_DEFAULT_LOCALE
): String {
    val formatter = DateTimeFormatter.ofPattern(
        DAY_MONTH_YEAR_TEXT_FIELD_PATTERN,
        LocaleUtils.getLocaleFromSelectedLanguage(selectedLanguage)
    )
    return localDate.format(formatter)
}

fun LocalDate?.toDisplayedDate(): String {
    return this?.let { convertLocalDateToFormattedString(it) }.orEmpty()
}

fun Instant.toLocalDateTime(zoneId: ZoneId = ZoneId.systemDefault()): LocalDateTime {
    return LocalDateTime.ofInstant(this, zoneId)
}

fun Instant.toLocalDate(zoneId: ZoneId = ZoneId.systemDefault()): LocalDate {
    return this.atZone(zoneId).toLocalDate()
}

fun utcMillisToLocalDate(utcMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): LocalDate {
    return Instant.ofEpochMilli(utcMillis).atZone(zoneId).toLocalDate()
}

fun localDateToUtcMillis(localDate: LocalDate): Long {
    return localDateToMillis(localDate = localDate, zoneId = ZoneOffset.UTC)
}

fun localDateToMillis(localDate: LocalDate, zoneId: ZoneId = ZoneId.systemDefault()): Long {
    return localDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
}

fun LocalDate.toLocalDateTime(time: LocalTime = LocalTime.MIDNIGHT): LocalDateTime {
    return this.atTime(time)
}