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
 * The user's per-match disclosure decision — the subset of [PresentationMatchDomain.requestedClaims]
 * to disclose, in pure domain fields (no Wallet Core types). At send-time the controller re-pairs
 * it to the raw match by `(documentId, credentialId, queryId)`, narrows that match's claims to
 * [selectedClaims], and supplies the per-credential key-unlock data.
 */
data class PresentationSelectionDomain(
    val documentId: String,
    val credentialId: String,
    val queryId: String?,
    val selectedClaims: Set<ClaimPathDomain>,
)