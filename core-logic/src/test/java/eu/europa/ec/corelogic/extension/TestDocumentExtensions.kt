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

import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.testlogic.extension.runTest
import eu.europa.ec.testlogic.rule.CoroutineTestRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.multipaz.credential.SecureAreaBoundCredential
import kotlin.time.Instant
import kotlin.time.toJavaInstant

class TestDocumentExtensions {

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    //region getExpiryDate

    @Test
    fun `Given several credentials, When getExpiryDate is called, Then the latest validUntil is returned`() {
        coroutineRule.runTest {
            // Given the batch's credentials expire on different dates.
            val earlier = Instant.parse("2030-01-01T00:00:00Z")
            val latest = Instant.parse("2035-01-01T00:00:00Z")
            val document = mockDocumentWithCredentials(earlier, latest)

            // When
            val result = document.getExpiryDate()

            // Then the document's expiry is the latest credential's validUntil.
            assertEquals(latest.toJavaInstant(), result)
        }
    }

    @Test
    fun `Given no credentials, When getExpiryDate is called, Then null is returned`() {
        coroutineRule.runTest {
            // Given
            val document = mockDocumentWithCredentials()

            // When / Then
            assertNull(document.getExpiryDate())
        }
    }

    //endregion

    //region isExpired

    @Test
    fun `Given the latest credential is in the past, When isExpired is called, Then true is returned`() {
        coroutineRule.runTest {
            // Given
            val document = mockDocumentWithCredentials(Instant.parse("2020-01-01T00:00:00Z"))

            // When / Then
            assertTrue(document.isExpired())
        }
    }

    @Test
    fun `Given the latest credential is in the future, When isExpired is called, Then false is returned`() {
        coroutineRule.runTest {
            // Given
            val document = mockDocumentWithCredentials(Instant.parse("2100-01-01T00:00:00Z"))

            // When / Then
            assertFalse(document.isExpired())
        }
    }

    @Test
    fun `Given no credentials, When isExpired is called, Then false is returned`() {
        coroutineRule.runTest {
            // Given
            val document = mockDocumentWithCredentials()

            // When / Then
            assertFalse(document.isExpired())
        }
    }

    //endregion

    //region helper functions

    private suspend fun mockDocumentWithCredentials(vararg validUntil: Instant): IssuedDocument {
        val credentials = validUntil.map { instant ->
            val credential = mock<SecureAreaBoundCredential>()
            whenever(credential.validUntil).thenReturn(instant)
            credential
        }
        val document = mock<IssuedDocument>()
        whenever(document.getCredentials()).thenReturn(credentials)
        return document
    }

    //endregion
}