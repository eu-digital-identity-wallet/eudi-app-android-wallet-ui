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

import java.util.Locale

object LocaleUtils {

    const val PROJECT_DEFAULT_LOCALE = "en-GB"

    /**
     *  Returns locale from string
     *  @param selectedLanguage example of selectedLanguage : en-GB
     *  @return [Locale] object
     */
    fun getLocaleFromSelectedLanguage(selectedLanguage: String): Locale {
        val (langFirstPart, langSecondPart) = selectedLanguage.split("-")
        return Locale(
            langFirstPart,
            langSecondPart
        )
    }
}