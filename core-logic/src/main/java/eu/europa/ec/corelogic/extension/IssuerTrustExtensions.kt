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

import androidx.annotation.StringRes
import eu.europa.ec.corelogic.model.IssuerTrustVerdictDomain
import eu.europa.ec.eudi.etsi1196x2.consultation.CertificationChainValidation
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.storagelogic.model.IssuerTrustVerdict

/**
 * Maps the issuer-trust result on an issuance event to the verdict the app stores. Wallet Core
 * returns `null` when it evaluated nothing: trust is not configured, or the attestation type is
 * not classified (in the reference configuration, everything except the PID).
 */
internal fun CertificationChainValidation<*>?.toIssuerTrustVerdict(): IssuerTrustVerdict =
    when (this) {
        is CertificationChainValidation.Trusted -> IssuerTrustVerdict.TRUSTED
        is CertificationChainValidation.NotTrusted -> IssuerTrustVerdict.NOT_TRUSTED
        null -> IssuerTrustVerdict.NOT_EVALUATED
    }

internal fun IssuerTrustVerdict.toIssuerTrustVerdictDomain(): IssuerTrustVerdictDomain =
    when (this) {
        IssuerTrustVerdict.TRUSTED -> IssuerTrustVerdictDomain.TRUSTED
        IssuerTrustVerdict.NOT_TRUSTED -> IssuerTrustVerdictDomain.NOT_TRUSTED
        IssuerTrustVerdict.NOT_EVALUATED -> IssuerTrustVerdictDomain.NOT_EVALUATED
    }

@StringRes
fun IssuerTrustVerdictDomain?.toIssuerVerificationTextResId(): Int =
    when (this) {
        IssuerTrustVerdictDomain.TRUSTED -> R.string.generic_issuer_verified
        IssuerTrustVerdictDomain.NOT_TRUSTED -> R.string.generic_issuer_trust_not_confirmed
        IssuerTrustVerdictDomain.NOT_EVALUATED -> R.string.generic_issuer_verification_not_performed
        null -> R.string.generic_issuer_verification_unavailable
    }