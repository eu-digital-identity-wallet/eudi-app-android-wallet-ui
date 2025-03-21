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

interface WalletCoreConfig {
    val config: EudiWalletConfig

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
}