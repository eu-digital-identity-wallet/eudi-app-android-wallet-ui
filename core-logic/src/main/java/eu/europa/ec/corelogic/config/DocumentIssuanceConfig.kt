/*
 * Copyright (c) 2023 European Commission
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

/**
 * Represents the configuration for document issuance.
 *
 * This class defines the rules for issuing documents, including a default rule and
 * specific rules for individual document types.
 *
 * @property defaultRule The default [DocumentIssuanceRule] to be applied when no specific rule
 *                       is defined for a document type.
 * @property documentSpecificRules A map where keys are [eu.europa.ec.corelogic.model.DocumentIdentifier]s and values are
 *                                  [DocumentIssuanceRule]s, defining specific rules for
 *                                  particular document types.
 */
data class DocumentIssuanceConfig(
    val defaultRule: DocumentIssuanceRule,
    val documentSpecificRules: Map<DocumentIdentifier, DocumentIssuanceRule>
) {

    /**
     * Retrieves the [DocumentIssuanceRule] for a given [DocumentIdentifier].
     *
     * If a specific rule is defined for the provided [documentIdentifier], that rule is returned.
     * Otherwise, the [defaultRule] is returned.
     *
     * @param documentIdentifier The identifier of the document for which to retrieve the rule.
     *                           If null, the [defaultRule] will be returned.
     * @return The [DocumentIssuanceRule] applicable to the given [documentIdentifier], or the
     *         [defaultRule] if no specific rule is found.
     */
    fun getRuleForDocument(documentIdentifier: DocumentIdentifier?): DocumentIssuanceRule =
        documentSpecificRules[documentIdentifier] ?: defaultRule
}

/**
 * Represents a rule for issuing a document.
 *
 * This class encapsulates the policy and the number of credentials associated with a document
 * issuance.
 *
 * @property policy The [CredentialPolicy] to be applied during document issuance.
 * @property numberOfCredentials The number of credentials to be issued for the document.
 */
data class DocumentIssuanceRule(
    val policy: CredentialPolicy,
    val numberOfCredentials: Int,
)