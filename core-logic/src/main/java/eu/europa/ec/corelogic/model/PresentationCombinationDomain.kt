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

import eu.europa.ec.corelogic.extension.toClaimPath
import eu.europa.ec.corelogic.model.PresentationMatchDomain.Companion.from
import org.multipaz.presentment.CredentialMatchSourceIso18013
import org.multipaz.presentment.CredentialMatchSourceOpenID4VP
import org.multipaz.presentment.CredentialPresentmentSetOptionMemberMatch

/**
 * One way to satisfy the verifier's request — the user picks one. A 1:1 projection of a Wallet Core
 * `presentmentSelections` entry; the Wallet Core SDK has already done the DCQL expansion, so the
 * app does none of its own.
 *
 * @property matches the documents disclosed for this combination, in Wallet Core's order.
 */
data class PresentationCombinationDomain(
    val matches: List<PresentationMatchDomain>,
)

/**
 * A flat, pure-domain snapshot of one Wallet Core SDK match — a candidate credential the verifier
 * asked for and the wallet can fulfil. Built via [from], so it holds no Wallet Core types; the raw
 * match stays in the controller.
 *
 * @property documentId Wallet Core's `Document.identifier`.
 * @property credentialId Wallet Core's `Credential.identifier`.
 * @property requestedClaims the claim paths the verifier asked for and the wallet matched, as the
 * app's own [ClaimPathDomain].
 */
data class PresentationMatchDomain(
    val documentId: String,
    val credentialId: String,
    val queryId: String?,
    val requestedClaims: List<ClaimPathDomain>,
) {
    companion object {
        fun from(match: CredentialPresentmentSetOptionMemberMatch): PresentationMatchDomain {
            val (documentId, credentialId, queryId) = match.identityKey
            return PresentationMatchDomain(
                documentId = documentId,
                credentialId = credentialId,
                queryId = queryId,
                requestedClaims = match.claims.keys.map { requestedClaim ->
                    requestedClaim.toClaimPath()
                },
            )
        }
    }
}

/**
 * Identity of a Wallet Core match as `(documentId, credentialId, queryId)` — the key the controller
 * re-pairs a [PresentationSelectionDomain] to its raw match by. `queryId` (the DCQL query id, null
 * for proximity and DC-API) separates two matches that hit the same credential via different queries.
 */
internal val CredentialPresentmentSetOptionMemberMatch.identityKey: Triple<String, String, String?>
    get() = Triple(
        credential.document.identifier,
        credential.identifier,
        when (val matchSource = source) {
            // OpenID4VP/DCQL — the query id is mandatory
            is CredentialMatchSourceOpenID4VP -> matchSource.credentialQuery.id
            // BLE proximity and DC-API : no DCQL, no query id
            is CredentialMatchSourceIso18013 -> null
        },
    )