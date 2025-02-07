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

package eu.europa.ec.businesslogic.extension

import java.util.Locale

/**
 * Compares the language part of this Locale with the language part of another Locale.
 *
 * This function checks if the language tag (e.g., "en", "es", "fr") of this Locale is the same as
 * the language tag of the [localeToCompare].  It returns `true` if they are equal, and `false` otherwise.
 * If [localeToCompare] is `null`, it returns `false`.
 *
 * @param localeToCompare The Locale to compare with. Can be `null`.
 * @return `true` if the language tags of the two Locales are the same, `false` otherwise.
 *
 * @receiver The Locale instance on which this extension function is called.
 */
fun Locale.compareLocaleLanguage(localeToCompare: Locale?): Boolean =
    localeToCompare?.let { this.language == it.language } == true

/**
 * Retrieves a localized string from a list of items based on the user's locale.
 *
 * This function attempts to find a string within a list of items that matches the user's
 * locale. Each item in the list is expected to have an associated locale and
 * a string value. If no match is found, a provided fallback string is used.
 *
 * @param T The type of the items in the list.
 * @param userLocale The user's locale to match against.
 * @param localeExtractor A lambda function that extracts the locale from an item in the list.
 * @param stringExtractor A lambda function that extracts the string value from an item in the list.
 * @param fallback The fallback string to use if no if no match is found.
 * @return The localized string if found, otherwise the fallback string.
 *
 * @receiver The list of items to search within.
 */
fun <T> List<T>?.getLocalizedString(
    userLocale: Locale,
    localeExtractor: (T) -> Locale?,
    stringExtractor: (T) -> String?,
    fallback: String,
): String {
    return this.getLocalizedValue(
        userLocale = userLocale,
        localeExtractor = localeExtractor,
        valueExtractor = stringExtractor,
        fallback = fallback,
    ) ?: fallback
}

/**
 * Retrieves a localized value from a list of items based on the user's locale.
 *
 * This function iterates through a list of items and attempts to find an item whose locale
 * matches the user's locale. If a match is found, the corresponding value is extracted
 * and returned. If no match is found, it returns the value extracted from the first item in the list.
 * If the list is empty or if any exception occurs during the process, a provided fallback value is returned.
 *
 * @param T The type of items in the list.
 * @param M The type of the value to be extracted.
 * @param userLocale The user's locale to match against.
 * @param localeExtractor A function that extracts the locale from an item of type [T].
 * @param valueExtractor A function that extracts the desired value of type [M] from an item of type [T].
 * @param fallback The fallback value to return if no match is found, the list is empty, or an exception occurs.
 * @return The localized value of type [M], or the fallback value if no match is found, the list is empty or an exception occured.
 */
fun <T, M> List<T>?.getLocalizedValue(
    userLocale: Locale,
    localeExtractor: (T) -> Locale?,
    valueExtractor: (T) -> M?,
    fallback: M?,
): M? {
    return try {
        // Match based on locale
        this?.find { userLocale.compareLocaleLanguage(localeExtractor(it)) }?.let(valueExtractor)
            ?: this?.firstOrNull()?.let(valueExtractor) // If no matches: Use the first available
            ?: fallback // If list is empty, return the fallback
    } catch (_: Exception) {
        fallback // If an exception occurs, return the fallback
    }
}