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

package eu.europa.ec.corelogic.extension

import eu.europa.ec.businesslogic.extension.getLocalizedString
import eu.europa.ec.eudi.openid4vci.Display
import eu.europa.ec.eudi.wallet.document.metadata.DocumentMetaData

import java.util.Locale

/**
 * Retrieves the localized claim name from a list of claim displays.
 *
 * This function searches through a list of [DocumentMetaData.Claim.Display] objects
 * to find the name that best matches the user's locale. If an exact
 * match is not found, it falls back to the provided [fallback] string.
 *
 * @param userLocale The user's locale to match against.
 * @param fallback The fallback string to use if no match is found.
 * @return The localized claim name as a string, or the [fallback] string if no
 * matching locale is found.
 *
 * @see getLocalizedString
 */
fun List<DocumentMetaData.Claim.Display>?.getLocalizedClaimName(
    userLocale: Locale,
    fallback: String,
): String {
    return this.getLocalizedString(
        userLocale = userLocale,
        localeExtractor = { it.locale },
        stringExtractor = { it.name },
        fallback = fallback,
    )
}

/**
 * Retrieves the localized display name from a list of [Display] objects based on the user's locale.
 *
 * This function searches through a list of [Display] objects to find a display name that matches
 * the provided [userLocale]. If a matching locale is found, the corresponding name is returned.
 * If no matching locale is found, the provided [fallback] string is returned.
 *
 * @param userLocale The user's locale to match against.
 * @param fallback The fallback string to use if no match is found.
 * @return The localized display name if found, otherwise the [fallback] string.
 *
 * @see getLocalizedString
 */
fun List<Display>.getLocalizedDisplayName(
    userLocale: Locale,
    fallback: String,
): String {
    return this.getLocalizedString(
        userLocale = userLocale,
        localeExtractor = { it.locale },
        stringExtractor = { it.name },
        fallback = fallback,
    )
}

/**
 * Retrieves the localized display name of a document based on the user's locale.
 *
 * This function attempts to find a localized version of the document's name
 * within the [DocumentMetaData.display] property of the [DocumentMetaData] object. If a localized
 * version matching the user's locale is found, it is returned. If no matching
 * localized version is found, a fallback string is returned instead.
 *
 * @param userLocale The user's locale, used to find a matching localized name.
 * @param fallback The string to return if no matching localized name is found.
 * @return The localized document name, or the fallback string if no matching localized name is available.
 * If [DocumentMetaData] or its display is null, it will return the [fallback].
 *
 * @see getLocalizedString
 */
fun DocumentMetaData?.getLocalizedDocumentName(
    userLocale: Locale,
    fallback: String,
): String {
    return this?.display.getLocalizedString(
        userLocale = userLocale,
        localeExtractor = { it.locale },
        stringExtractor = { it.name },
        fallback = fallback,
    )
}