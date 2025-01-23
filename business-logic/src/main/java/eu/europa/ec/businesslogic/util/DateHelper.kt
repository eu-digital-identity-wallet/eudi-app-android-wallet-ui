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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
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

fun Instant.formatInstant(
    zoneId: ZoneId = ZoneId.systemDefault(),
    locale: Locale = Locale.ENGLISH
): String {
    val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy", locale)
        .withZone(zoneId)
    return formatter.format(this)
}