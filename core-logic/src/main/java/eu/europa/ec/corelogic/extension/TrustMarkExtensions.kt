/*
 * Copyright (c) 2026 European Commission
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

import eu.europa.ec.businesslogic.extension.getLocalizedValue
import eu.europa.ec.businesslogic.util.LocaleUtils
import eu.europa.ec.eudi.wallet.trustmark.TrustMarkResource
import java.util.Locale

fun TrustMarkResource.Text.getLocalizedText(userLocale: Locale): String? {
    return localisations.entries
        .filter { translation ->
            translation.value.isNotBlank()
        }
        .getLocalizedValue(
            userLocale = userLocale,
            localeExtractor = { translation ->
                LocaleUtils.getLocaleFromSelectedLanguage(
                    selectedLanguage = translation.key.replace(
                        oldChar = '_',
                        newChar = '-'
                    )
                )
            },
            valueExtractor = { translation -> translation.value },
            fallback = null,
        )
}