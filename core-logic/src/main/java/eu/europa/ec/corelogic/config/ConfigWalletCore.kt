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

import eu.europa.ec.corelogic.model.DocumentCategories
import eu.europa.ec.corelogic.model.DocumentCategory
import eu.europa.ec.corelogic.model.DocumentIdentifier
import eu.europa.ec.eudi.wallet.EudiWalletConfig
import java.time.Duration

interface WalletCoreConfig {


    /**
     * Holds the configuration settings for the EudiWallet.
     * This configuration includes settings such as API endpoints, cryptographic parameters,
     * and storage locations.
     */
    val config: EudiWalletConfig

    /**
     * Returns a predefined set of document categories and their associated identifiers.
     *
     * This property provides a structured mapping of common document categories (e.g., Government, Travel, Finance)
     * to specific document identifiers.  These identifiers are used to uniquely identify different types of
     * documents within a given category and are based on standard formats and specifications.
     *
     * The structure is a map where:
     * - Keys are [DocumentCategory] enum values representing broad document types.
     * - Values are lists of [DocumentIdentifier] objects, each representing a specific document type within that category.
     *
     * The supported document identifiers utilize various formats, including:
     * - Predefined types like [DocumentIdentifier.MdocPid] and [DocumentIdentifier.SdJwtPid].
     * - Formats defined by ISO standards (e.g., "org.iso.18013.5.1.mDL", "org.iso.23220.2.photoid.1").
     * - Formats specified by the European Union (e.g., "eu.europa.ec.eudi.tax.1", "eu.europa.ec.eudi.iban.1").
     * - URNs following the "urn:eu.europa.ec.eudi" namespace (e.g., "urn:eu.europa.ec.eudi:tax:1", "urn:eu.europa.ec.eudi:iban:1").
     *
     * The "Other" category is used for identifiers that don't fit neatly into the other predefined categories.
     *
     * Note:  An empty list for a category (e.g., Education) indicates that no specific document identifiers are
     * currently defined for that category.
     */
    val documentCategories: DocumentCategories
        get() = DocumentCategories(
            value = mapOf(
                DocumentCategory.Government to listOf(
                    DocumentIdentifier.MdocPid,
                    DocumentIdentifier.SdJwtPid,
                    DocumentIdentifier.OTHER(
                        formatType = "org.iso.18013.5.1.mDL"
                    ),
                    DocumentIdentifier.OTHER(
                        formatType = "eu.europa.ec.eudi.tax.1"
                    ),
                    DocumentIdentifier.OTHER(
                        formatType = "urn:eu.europa.ec.eudi:tax:1"
                    ),
                    DocumentIdentifier.OTHER(
                        formatType = "eu.europa.ec.eudi.pseudonym.age_over_18.1"
                    ),
                    DocumentIdentifier.OTHER(
                        formatType = "urn:eu.europa.ec.eudi:pseudonym_age_over_18:1"
                    ),
                    DocumentIdentifier.OTHER(
                        formatType = "eu.europa.ec.eudi.pseudonym.age_over_18.deferred_endpoint"
                    ),
                    DocumentIdentifier.OTHER(
                        formatType = "eu.europa.ec.eudi.cor.1"
                    ),
                ),
                DocumentCategory.Travel to listOf(
                    DocumentIdentifier.OTHER(
                        formatType = "org.iso.23220.2.photoid.1"
                    ),
                    DocumentIdentifier.OTHER(
                        formatType = "org.iso.23220.photoID.1"
                    ),
                    DocumentIdentifier.OTHER(
                        formatType = "org.iso.18013.5.1.reservation"
                    ),
                ),
                DocumentCategory.Finance to listOf(
                    DocumentIdentifier.OTHER(
                        formatType = "eu.europa.ec.eudi.iban.1"
                    ),
                    DocumentIdentifier.OTHER(
                        formatType = "urn:eu.europa.ec.eudi:iban:1"
                    ),
                ),
                DocumentCategory.Education to emptyList(),
                DocumentCategory.Health to listOf(
                    DocumentIdentifier.OTHER(
                        formatType = "eu.europa.ec.eudi.hiid.1"
                    ),
                    DocumentIdentifier.OTHER(
                        formatType = "urn:eu.europa.ec.eudi:hiid:1"
                    ),
                    DocumentIdentifier.OTHER(
                        formatType = "eu.europa.ec.eudi.ehic.1"
                    ),
                    DocumentIdentifier.OTHER(
                        formatType = "urn:eu.europa.ec.eudi:ehic:1"
                    ),
                ),
                DocumentCategory.SocialSecurity to listOf(
                    DocumentIdentifier.OTHER(
                        formatType = "eu.europa.ec.eudi.pda1.1"
                    ),
                    DocumentIdentifier.OTHER(
                        formatType = "urn:eu.europa.ec.eudi:pda1:1"
                    ),
                ),
                DocumentCategory.Retail to listOf(
                    DocumentIdentifier.OTHER(
                        formatType = "eu.europa.ec.eudi.loyalty.1"
                    ),
                    DocumentIdentifier.OTHER(
                        formatType = "eu.europa.ec.eudi.msisdn.1"
                    ),
                    DocumentIdentifier.OTHER(
                        formatType = "urn:eu.europa.ec.eudi:msisdn:1"
                    ),
                ),
                DocumentCategory.Other to listOf(
                    DocumentIdentifier.OTHER(
                        formatType = "eu.europa.ec.eudi.por.1"
                    ),
                    DocumentIdentifier.OTHER(
                        formatType = "urn:eu.europa.ec.eudi:por:1"
                    ),
                ),
            )
        )

    /**
     * The interval at which revocations are checked.
     *
     * This property defines the time interval between checks for revoked tokens or credentials.
     * It is currently set to 15 minutes.
     */
    val revocationInterval: Duration get() = Duration.ofMinutes(15)
}