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

import eu.europa.ec.corelogic.model.RecordedRegistrationCheckDomain
import eu.europa.ec.corelogic.model.RegistrationDetailsDomain
import eu.europa.ec.corelogic.model.RegistrationFailureReasonDomain
import eu.europa.ec.corelogic.model.RegistrationStatusDomain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TestRecordedRegistrationCheckExtensions {

    @Test
    fun `Given every recordable check, When it is stored and read back, Then it is unchanged`() {
        val checks = listOf(
            RecordedRegistrationCheckDomain.Verified,
            RecordedRegistrationCheckDomain.NotEvaluated
        ) +
                RegistrationFailureReasonDomain.entries.map { reason ->
                    RecordedRegistrationCheckDomain.NotVerified(reason = reason)
                }

        checks.forEach { check ->
            assertEquals(
                check,
                recordedRegistrationCheckOf(
                    check = check.toStoredRegistrationCheck(),
                    failureReason = check.toStoredRegistrationFailureReason(),
                ),
            )
        }
    }

    @Test
    fun `Given nothing recorded or an unknown value, When it is read, Then the check is unknown`() {
        assertEquals(
            RecordedRegistrationCheckDomain.Unknown,
            recordedRegistrationCheckOf(null, null)
        )
        assertEquals(
            RecordedRegistrationCheckDomain.Unknown,
            recordedRegistrationCheckOf("SOMETHING", null)
        )
        assertNull(RecordedRegistrationCheckDomain.Unknown.toStoredRegistrationCheck())
    }

    @Test
    fun `Given a failed check with an unknown reason, When it is read, Then it is not verified without a reason`() {
        assertEquals(
            RecordedRegistrationCheckDomain.NotVerified(reason = null),
            recordedRegistrationCheckOf(check = "NOT_VERIFIED", failureReason = "RETIRED_REASON"),
        )
    }

    @Test
    fun `Given each registration outcome, When it is recorded, Then the matching check results`() {
        assertEquals(
            RecordedRegistrationCheckDomain.Verified,
            RegistrationStatusDomain.Verified(
                details = RegistrationDetailsDomain(
                    tradeName = null,
                    uniqueId = null,
                    logoUri = null,
                    intendedUse = null,
                    privacyPolicyUrl = null,
                    serviceDescription = null,
                ),
                overaskedClaims = emptyList(),
            ).toRecordedRegistrationCheck(),
        )
        assertEquals(
            RecordedRegistrationCheckDomain.NotVerified(reason = RegistrationFailureReasonDomain.REVOKED),
            RegistrationStatusDomain.NotVerified(
                reason = RegistrationFailureReasonDomain.REVOKED,
                details = null,
            ).toRecordedRegistrationCheck(),
        )
        assertEquals(
            RecordedRegistrationCheckDomain.NotEvaluated,
            RegistrationStatusDomain.NotEvaluated.toRecordedRegistrationCheck(),
        )
    }
}