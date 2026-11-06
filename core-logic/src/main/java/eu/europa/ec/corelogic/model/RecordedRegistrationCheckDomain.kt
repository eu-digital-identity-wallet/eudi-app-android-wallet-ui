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

/**
 * What the wallet established about a relying party's registration when it shared data with it,
 * recorded beside the transaction-log entry because Wallet Core does not keep it there.
 */
sealed interface RecordedRegistrationCheckDomain {

    data object Verified : RecordedRegistrationCheckDomain

    /** The registration failed its checks; [reason] is null when the recorded reason is not known. */
    data class NotVerified(val reason: RegistrationFailureReasonDomain?) :
        RecordedRegistrationCheckDomain

    /** The registration was not checked, for example because checking is turned off. */
    data object NotEvaluated : RecordedRegistrationCheckDomain

    /** No outcome was recorded with the entry. Its registration counts as not verified. */
    data object Unknown : RecordedRegistrationCheckDomain

    /** True when the registration certificate was issued to another party than the one that asked. */
    val isNotBoundToRequester: Boolean
        get() = (this as? NotVerified)?.reason == RegistrationFailureReasonDomain.NOT_BOUND_TO_REQUESTER
}