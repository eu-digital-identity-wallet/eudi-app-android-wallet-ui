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

package eu.europa.ec.commonfeature.ui.request.model

import eu.europa.ec.businesslogic.extension.ifEmptyOrNull
import eu.europa.ec.corelogic.model.RegistrationStatusDomain
import eu.europa.ec.corelogic.model.RelyingPartyDomain
import eu.europa.ec.uilogic.component.RelyingPartyDataUi

/**
 * The request screen's who-is-asking header: the requester's identity block and the registration
 * sections (hidden when null).
 */
data class RelyingPartyHeaderUi(
    val relyingParty: RelyingPartyDataUi,
    val intendedUse: String?,
    val privacyPolicyUrl: String?,
)

/**
 * Projects the domain relying party into the request screen's header. The badge comes from
 * [isFullyVerified] — the same verdict every other screen of the presentation renders — while
 * registration details render whenever a certificate was parsed, even one that failed validation.
 */
fun RelyingPartyDomain.toRelyingPartyHeaderUi(fallbackName: String): RelyingPartyHeaderUi {
    val safeRegistration = registration

    val showVerifiedBadge = isFullyVerified

    val details = when (safeRegistration) {
        is RegistrationStatusDomain.Verified -> safeRegistration.details
        is RegistrationStatusDomain.NotVerified -> safeRegistration.details
        is RegistrationStatusDomain.NotEvaluated -> null
    }

    return RelyingPartyHeaderUi(
        relyingParty = RelyingPartyDataUi(
            logo = logoUri,
            isVerified = showVerifiedBadge,
            name = name.ifEmptyOrNull(default = fallbackName),
            uniqueId = uniqueId,
            description = null,
        ),
        intendedUse = details?.intendedUse,
        privacyPolicyUrl = details?.privacyPolicyUrl,
    )
}

/**
 * The acknowledge banner of a request with registration problems; Share stays disabled until
 * the user flips [riskAccepted].
 */
data class RegistrationWarningUi(
    val variant: RegistrationWarningVariantUi,
    val riskAccepted: Boolean,
)

enum class RegistrationWarningVariantUi {
    /** The registration could not be obtained or validated. */
    NOT_VERIFIED,

    /** The request asks for data the registration does not cover. */
    OVERASKED,
}

/**
 * The banner the registration outcome demands, or null when none. Always starts
 * unacknowledged — the user must flip the switch anew for every rendered request.
 */
fun RelyingPartyDomain.toRegistrationWarningUi(): RegistrationWarningUi? {
    return when (val safeRegistration = registration) {
        is RegistrationStatusDomain.Verified -> {
            if (safeRegistration.overaskedClaims.isNotEmpty()) {
                RegistrationWarningUi(
                    variant = RegistrationWarningVariantUi.OVERASKED,
                    riskAccepted = false,
                )
            } else {
                null
            }
        }

        is RegistrationStatusDomain.NotVerified -> RegistrationWarningUi(
            variant = RegistrationWarningVariantUi.NOT_VERIFIED,
            riskAccepted = false,
        )

        is RegistrationStatusDomain.NotEvaluated -> null
    }
}