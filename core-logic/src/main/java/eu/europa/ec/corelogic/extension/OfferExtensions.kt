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

package eu.europa.ec.corelogic.extension

import eu.europa.ec.businesslogic.extension.getLocalizedString
import eu.europa.ec.businesslogic.extension.getLocalizedValue
import eu.europa.ec.corelogic.model.DocumentIdentifier
import eu.europa.ec.corelogic.model.toDocumentIdentifier
import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcFormat
import eu.europa.ec.eudi.wallet.issue.openid4vci.Offer
import java.net.URI
import java.util.Locale

fun Offer.getIssuerName(locale: Locale): String {
    return issuerMetadata.display.getLocalizedString(
        userLocale = locale,
        localeExtractor = {
            it.locale?.let { Locale(it) }
        },
        stringExtractor = { it.name },
        fallback = issuerMetadata.credentialIssuerIdentifier.value.value.host
    )
}

fun Offer.getIssuerLogo(locale: Locale): URI? {
    return issuerMetadata.display.getLocalizedValue(
        userLocale = locale,
        localeExtractor = {
            it.locale?.let { Locale(it) }
        },
        valueExtractor = { it.logo?.uri },
        fallback = null
    )
}

val Offer.OfferedDocument.documentIdentifier: DocumentIdentifier?
    get() = when (val format = documentFormat) {
        is MsoMdocFormat -> format.docType.toDocumentIdentifier()
        is SdJwtVcFormat -> format.vct.toDocumentIdentifier()
        null -> null
    }

fun Offer.OfferedDocument.getName(locale: Locale): String? {
    return configuration.display.getLocalizedValue(
        userLocale = locale,
        localeExtractor = { it.locale },
        valueExtractor = { it.name },
        fallback = when (val format = documentFormat) {
            is MsoMdocFormat -> format.docType
            is SdJwtVcFormat -> format.vct
            null -> null
        }
    )
}