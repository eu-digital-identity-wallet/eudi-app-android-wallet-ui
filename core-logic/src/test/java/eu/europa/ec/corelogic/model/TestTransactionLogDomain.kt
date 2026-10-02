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

package eu.europa.ec.corelogic.model

import eu.europa.ec.corelogic.util.mockedClaimSegments
import eu.europa.ec.corelogic.util.mockedCredentialType
import eu.europa.ec.corelogic.util.mockedTransactionId
import eu.europa.ec.corelogic.util.mockedTransactionTime
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneOffset

class TestTransactionLogDomain {

    //region canRequestDataDeletion

    // Case 1:
    // 1. A completed presentation has recorded disclosed claims.
    //
    // Case 1 Expected Result:
    // A data deletion request can be initiated.
    @Test
    fun `Given Case 1, When canRequestDataDeletion is read, Then Case 1 Expected Result is returned`() {
        // Given
        val presentation = mockedPresentation.copy(claimsPresented = listOf(mockedCredentialClaims))

        // When
        val result = presentation.canRequestDataDeletion

        // Then
        assertTrue(result)
    }

    // Case 2:
    // 1. A presentation did not complete but recorded disclosed claims.
    //
    // Case 2 Expected Result:
    // Its final result does not prevent a data deletion request.
    @Test
    fun `Given Case 2, When canRequestDataDeletion is read, Then Case 2 Expected Result is returned`() {
        // Given
        val presentation = mockedPresentation.copy(
            result = TransactionResultDomain.NotCompleted(reason = null),
            claimsPresented = listOf(mockedCredentialClaims),
        )

        // When
        val result = presentation.canRequestDataDeletion

        // Then
        assertTrue(result)
    }

    // Case 3:
    // 1. A presentation requested claims but recorded no disclosure.
    //
    // Case 3 Expected Result:
    // Requested claims are not treated as disclosed claims.
    @Test
    fun `Given Case 3, When canRequestDataDeletion is read, Then Case 3 Expected Result is returned`() {
        // Given
        val presentation = mockedPresentation

        // When
        val result = presentation.canRequestDataDeletion

        // Then
        assertFalse(result)
    }

    // Case 4:
    // 1. Presented credential groups contain no claims.
    //
    // Case 4 Expected Result:
    // An empty group is not evidence of attribute disclosure.
    @Test
    fun `Given Case 4, When canRequestDataDeletion is read, Then Case 4 Expected Result is returned`() {
        // Given
        val presentation = mockedPresentation.copy(
            claimsPresented = listOf(mockedCredentialClaims.copy(claims = emptyList())),
        )

        // When
        val result = presentation.canRequestDataDeletion

        // Then
        assertFalse(result)
    }

    // Case 5:
    // 1. An empty presented group precedes a group with a disclosed claim.
    //
    // Case 5 Expected Result:
    // The recorded disclosure permits a data deletion request.
    @Test
    fun `Given Case 5, When canRequestDataDeletion is read, Then Case 5 Expected Result is returned`() {
        // Given
        val presentation = mockedPresentation.copy(
            claimsPresented = listOf(
                mockedCredentialClaims.copy(claims = emptyList()),
                mockedCredentialClaims,
            ),
        )

        // When
        val result = presentation.canRequestDataDeletion

        // Then
        assertTrue(result)
    }

    //endregion

    //region mocked objects
    private val mockedCredentialClaims = CredentialClaimsDomain(
        credential = CredentialRefDomain(identifier = mockedCredentialType),
        claims = listOf(ClaimRefDomain(segments = mockedClaimSegments)),
    )
    private val mockedPresentation = TransactionLogDomain.Presentation(
        transactionData = emptyList(),
        id = mockedTransactionId,
        time = mockedTransactionTime.atZone(ZoneOffset.UTC).toLocalDateTime(),
        result = TransactionResultDomain.Completed,
        party = InteractingPartyDomain(name = null, identifier = null, contacts = emptyList()),
        partyType = null,
        intermediary = null,
        registration = null,
        claimsRequested = listOf(mockedCredentialClaims),
        claimsPresented = emptyList(),
    )
    //endregion
}