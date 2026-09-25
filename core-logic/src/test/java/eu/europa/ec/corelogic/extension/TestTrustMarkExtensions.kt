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

import eu.europa.ec.eudi.wallet.trustmark.TrustMarkResource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class TestTrustMarkExtensions {

    //region getLocalizedText

    // Case 1:
    // 1. The requested language is available after another language.
    // Case 1 Expected Result:
    // The requested translation is selected.
    @Test
    fun `Given Case 1, When getLocalizedText is called, Then Case 1 Expected Result is returned`() {
        // Given
        val text = mockedText

        // When
        val result = text.getLocalizedText(userLocale = Locale.FRENCH)

        // Then
        assertEquals(mockedFrenchText, result)
    }

    // Case 2:
    // 1. The matching language tag has mixed case and an underscore.
    // Case 2 Expected Result:
    // The regional translation is selected with its whitespace unchanged.
    @Test
    fun `Given Case 2, When getLocalizedText is called, Then Case 2 Expected Result is returned`() {
        // Given
        val text = mockedText.copy(
            localisations = mapOf("en" to mockedEnglishText, "FR_ca" to mockedRegionalText)
        )

        // When
        val result = text.getLocalizedText(userLocale = Locale.CANADA_FRENCH)

        // Then
        assertEquals(mockedRegionalText, result)
    }

    // Case 3:
    // 1. A base-language translation precedes an exact regional translation.
    // Case 3 Expected Result:
    // The shared language-matching rule selects the first matching entry.
    @Test
    fun `Given Case 3, When getLocalizedText is called, Then Case 3 Expected Result is returned`() {
        // Given
        val text = mockedText.copy(
            localisations = mapOf("fr" to mockedFrenchText, "fr-CA" to mockedRegionalText)
        )

        // When
        val result = text.getLocalizedText(userLocale = Locale.CANADA_FRENCH)

        // Then
        assertEquals(mockedFrenchText, result)
    }

    // Case 4:
    // 1. The first matching translation is blank, and another nonblank match exists.
    // Case 4 Expected Result:
    // The nonblank matching translation is selected.
    @Test
    fun `Given Case 4, When getLocalizedText is called, Then Case 4 Expected Result is returned`() {
        // Given
        val text = mockedText.copy(
            localisations = mapOf(
                "en" to mockedEnglishText,
                "fr-CA" to " ",
                "fr" to mockedFrenchText
            )
        )

        // When
        val result = text.getLocalizedText(userLocale = Locale.CANADA_FRENCH)

        // Then
        assertEquals(mockedFrenchText, result)
    }

    // Case 5:
    // 1. The requested language is absent; a blank entry precedes French and English.
    // Case 5 Expected Result:
    // The first nonblank translation is selected without preferring English.
    @Test
    fun `Given Case 5, When getLocalizedText is called, Then Case 5 Expected Result is returned`() {
        // Given
        val text = mockedText.copy(
            localisations = mapOf("de" to " ", "fr" to mockedFrenchText, "en" to mockedEnglishText)
        )

        // When
        val result = text.getLocalizedText(userLocale = Locale.ITALIAN)

        // Then
        assertEquals(mockedFrenchText, result)
    }

    // Case 6:
    // 1. Translations are empty or all blank, and the resource has a name.
    // Case 6 Expected Result:
    // Null is returned without using the resource name as certification text.
    @Test
    fun `Given Case 6, When getLocalizedText is called, Then Case 6 Expected Result is returned`() {
        // Given
        val translations = listOf(emptyMap(), mapOf("en" to " ", "fr" to ""))
        translations.forEach { localisations ->
            val text = mockedText.copy(localisations = localisations)

            // When
            val result = text.getLocalizedText(userLocale = Locale.ENGLISH)

            // Then
            assertNull(result)
        }
    }

    // Case 7:
    // 1. Translation keys include regions, scripts, variants, extensions and mixed case.
    // Case 7 Expected Result:
    // Valid keys remain usable by the shared language-matching helper.
    @Test
    fun `Given Case 7, When getLocalizedText is called, Then Case 7 Expected Result is returned`() {
        // Given
        val locales = listOf(
            "fr-CA" to Locale.CANADA_FRENCH,
            "FR-ca" to Locale.CANADA_FRENCH,
            "sr-Latn-RS" to Locale.forLanguageTag("sr-RS"),
            "de-DE-1996" to Locale.GERMAN,
            "fr-CA-u-nu-latn" to Locale.CANADA_FRENCH,
            "fr-CA-x-wallet" to Locale.CANADA_FRENCH,
        )
        locales.forEach { (languageTag, userLocale) ->
            val text = mockedText.copy(
                localisations = mapOf("en" to mockedEnglishText, languageTag to mockedLocalizedText)
            )

            // When
            val result = text.getLocalizedText(userLocale = userLocale)

            // Then
            assertEquals(mockedLocalizedText, result)
        }
    }

    // Case 8:
    // 1. An unreadable language key precedes a usable matching translation.
    // Case 8 Expected Result:
    // The matching translation is still selected.
    @Test
    fun `Given Case 8, When getLocalizedText is called, Then Case 8 Expected Result is returned`() {
        // Given
        val languageTags = listOf("", "???", "_")
        languageTags.forEach { languageTag ->
            val text = mockedText.copy(
                localisations = mapOf(languageTag to mockedEnglishText, "fr" to mockedFrenchText)
            )

            // When
            val result = text.getLocalizedText(userLocale = Locale.FRENCH)

            // Then
            assertEquals(mockedFrenchText, result)
        }
    }

    //endregion

    //region test data

    private val mockedEnglishText = "Certification text"
    private val mockedFrenchText = "Texte de certification"
    private val mockedRegionalText = "  Texte régional  "
    private val mockedLocalizedText = "Selected translation"
    private val mockedText = TrustMarkResource.Text(
        name = "Resource description",
        localisations = mapOf("en" to mockedEnglishText, "fr" to mockedFrenchText),
    )

    //endregion
}