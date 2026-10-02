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

package eu.europa.ec.commonfeature.config

import eu.europa.ec.uilogic.serializer.UiSerializerImpl
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TestTrustMarkUiConfig {

    private val uiSerializer = UiSerializerImpl()

    //region serialization

    // Case 1:
    // 1. Welcome carries a continuation containing encoded navigation arguments.
    // Case 1 Expected Result:
    // The app serializer preserves the mode and complete continuation route.
    @Test
    fun `Given Case 1, When Welcome is serialized, Then Case 1 Expected Result is returned`() {
        // Given
        val config = TrustMarkUiConfig(
            mode = TrustMarkMode.Welcome(continuationRoute = mockedContinuationRoute)
        )

        // When
        val encoded = uiSerializer.toBase64(config, TrustMarkUiConfig.Parser)
        val decoded = uiSerializer.fromBase64(
            encoded, TrustMarkUiConfig::class.java, TrustMarkUiConfig.Parser,
        )

        // Then
        assertNotNull(encoded)
        assertEquals(config, decoded)
    }

    // Case 2:
    // 1. About has no continuation route.
    // Case 2 Expected Result:
    // The app serializer restores About without introducing Welcome state.
    @Test
    fun `Given Case 2, When About is serialized, Then Case 2 Expected Result is returned`() {
        // Given
        val config = TrustMarkUiConfig(mode = TrustMarkMode.About)

        // When
        val encoded = uiSerializer.toBase64(config, TrustMarkUiConfig.Parser)
        val decoded = uiSerializer.fromBase64(
            encoded, TrustMarkUiConfig::class.java, TrustMarkUiConfig.Parser,
        )

        // Then
        assertNotNull(encoded)
        assertEquals(config, decoded)
    }

    //endregion

    private val mockedContinuationRoute = "BIOMETRIC?biometricConfig=eyJ0eXBlIjoiTG9naW4ifQ=="
}