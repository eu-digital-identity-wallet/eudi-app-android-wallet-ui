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

package eu.europa.ec.commonfeature.config

import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.uilogic.config.ConfigNavigation
import eu.europa.ec.uilogic.serializer.UiSerializable
import eu.europa.ec.uilogic.serializer.UiSerializableParser
import kotlinx.serialization.Serializable

/**
 * The issuance success screen: the documents in [documentIds] were issued, and those named in
 * [notIssuedDocuments] were requested but not added. That covers the documents the issuer did not
 * issue and those refused because their issuer could not be trusted, which is why it is not named
 * after Wallet Core's `nonIssuedDocuments`, which holds only the first kind.
 */
@Serializable
data class IssuanceSuccessUiConfig(
    val documentIds: List<DocumentId>,
    val onSuccessNavigation: ConfigNavigation,
    val notIssuedDocuments: List<String> = emptyList(),
) : UiSerializable {

    companion object Parser : UiSerializableParser {
        override val serializedKeyName = "issuanceSuccessConfig"
    }
}