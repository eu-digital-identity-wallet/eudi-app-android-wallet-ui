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
import eu.europa.ec.corelogic.model.RegistrationFailureReasonDomain
import eu.europa.ec.corelogic.model.RegistrationStatusDomain

private const val CHECK_VERIFIED = "VERIFIED"
private const val CHECK_NOT_VERIFIED = "NOT_VERIFIED"
private const val CHECK_NOT_EVALUATED = "NOT_EVALUATED"

/** The outcome of the registration check of a presentation, as it is recorded. */
internal fun RegistrationStatusDomain.toRecordedRegistrationCheck(): RecordedRegistrationCheckDomain =
    when (this) {
        is RegistrationStatusDomain.Verified -> RecordedRegistrationCheckDomain.Verified
        is RegistrationStatusDomain.NotVerified -> RecordedRegistrationCheckDomain.NotVerified(
            reason = reason
        )

        RegistrationStatusDomain.NotEvaluated -> RecordedRegistrationCheckDomain.NotEvaluated
    }

/** The stored form of this check, or null for [RecordedRegistrationCheckDomain.Unknown]. */
internal fun RecordedRegistrationCheckDomain.toStoredRegistrationCheck(): String? = when (this) {
    RecordedRegistrationCheckDomain.Verified -> CHECK_VERIFIED
    is RecordedRegistrationCheckDomain.NotVerified -> CHECK_NOT_VERIFIED
    RecordedRegistrationCheckDomain.NotEvaluated -> CHECK_NOT_EVALUATED
    RecordedRegistrationCheckDomain.Unknown -> null
}

/** The stored reason of a failed check. */
internal fun RecordedRegistrationCheckDomain.toStoredRegistrationFailureReason(): String? =
    (this as? RecordedRegistrationCheckDomain.NotVerified)?.reason?.name

/** The check recorded as [check] and [failureReason]; anything else reads as not recorded. */
internal fun recordedRegistrationCheckOf(
    check: String?,
    failureReason: String?,
): RecordedRegistrationCheckDomain = when (check) {
    CHECK_VERIFIED -> RecordedRegistrationCheckDomain.Verified
    CHECK_NOT_VERIFIED -> RecordedRegistrationCheckDomain.NotVerified(
        reason = RegistrationFailureReasonDomain.entries.firstOrNull { reason -> reason.name == failureReason },
    )

    CHECK_NOT_EVALUATED -> RecordedRegistrationCheckDomain.NotEvaluated
    else -> RecordedRegistrationCheckDomain.Unknown
}