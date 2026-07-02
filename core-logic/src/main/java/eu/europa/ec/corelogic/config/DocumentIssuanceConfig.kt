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

package eu.europa.ec.corelogic.config

import eu.europa.ec.corelogic.model.DocumentIdentifier
import eu.europa.ec.eudi.wallet.document.CreateDocumentSettings.CredentialPolicy
import kotlin.time.Duration

/**
 * Represents the configuration for document issuance.
 *
 * This class defines the [CredentialPolicy] to apply per document type — a default policy plus
 * specific policies for individual document types — and the background re-issuance rule.
 *
 * @property defaultPolicy The default [CredentialPolicy] to apply when no specific policy is defined
 * for a document type.
 * @property documentSpecificPolicies A map where keys are [DocumentIdentifier]s and values are the
 * [CredentialPolicy] to apply for those specific document types.
 * @property reissuanceRule The rule for the automated document re-issuance background operation.
 */
data class DocumentIssuanceConfig(
    val defaultPolicy: CredentialPolicy,
    val documentSpecificPolicies: Map<DocumentIdentifier, CredentialPolicy>,
    val reissuanceRule: ReIssuanceRule
) {

    /**
     * Retrieves the [CredentialPolicy] for a given [DocumentIdentifier].
     *
     * If a specific policy is defined for the provided [documentIdentifier], that policy is returned.
     * Otherwise, the [defaultPolicy] is returned.
     *
     * @param documentIdentifier The identifier of the document for which to retrieve the policy.
     * If null, the [defaultPolicy] will be returned.
     * @return The [CredentialPolicy] applicable to the given [documentIdentifier], or the
     * [defaultPolicy] if no specific policy is found.
     */
    fun getPolicyForDocument(documentIdentifier: DocumentIdentifier?): CredentialPolicy =
        documentSpecificPolicies[documentIdentifier] ?: defaultPolicy
}

/**
 * Defines the configuration for the background process responsible for automatic document re-issuance.
 *
 * This rule manages the scheduling of the background operation. While the specific threshold
 * for re-issuance is governed by each document's [CredentialPolicy], this class specifies
 * how frequently the system should check for and process documents that need re-issuing.
 *
 * @property backgroundInterval The duration of time between consecutive executions of the
 * background re-issuance task.
 */
data class ReIssuanceRule(
    val backgroundInterval: Duration
)