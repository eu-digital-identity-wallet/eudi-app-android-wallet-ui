/*
 * Copyright (c) 2025 European Commission
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

package eu.europa.ec.uilogic.navigation.helper

import android.net.Uri
import eu.europa.ec.uilogic.BuildConfig

data class DeepLinkAction(
    val link: Uri,
    val type: DeepLinkType
)

enum class DeepLinkType(
    val schemas: List<String>,
    val host: String? = null
) {
    OPENID4VP(
        schemas = listOf(
            BuildConfig.OPENID4VP_SCHEME,
            BuildConfig.EUDI_OPENID4VP_SCHEME,
            BuildConfig.MDOC_OPENID4VP_SCHEME,
            BuildConfig.HAIP_OPENID4VP_SCHEME
        )
    ),
    CREDENTIAL_OFFER(
        schemas = listOf(
            BuildConfig.CREDENTIAL_OFFER_SCHEME,
            BuildConfig.CREDENTIAL_OFFER_HAIP_SCHEME
        )
    ),
    ISSUANCE(
        schemas = listOf(BuildConfig.ISSUE_AUTHORIZATION_SCHEME),
        host = BuildConfig.ISSUE_AUTHORIZATION_HOST
    ),
    EXTERNAL(
        emptyList()
    ),
    DYNAMIC_PRESENTATION(
        emptyList()
    ),
    RQES(
        schemas = listOf(BuildConfig.RQES_SCHEME),
        host = BuildConfig.RQES_HOST
    ),
    RQES_DOC_RETRIEVAL(
        schemas = listOf(BuildConfig.RQES_DOC_RETRIEVAL_SCHEME)
    );

    companion object {
        fun parse(scheme: String, host: String? = null): DeepLinkType = when {

            OPENID4VP.schemas.contains(scheme) -> {
                OPENID4VP
            }

            CREDENTIAL_OFFER.schemas.contains(scheme) -> {
                CREDENTIAL_OFFER
            }

            ISSUANCE.schemas.contains(scheme) && host == ISSUANCE.host -> {
                ISSUANCE
            }

            RQES.schemas.contains(scheme) && host == RQES.host -> {
                RQES
            }

            RQES_DOC_RETRIEVAL.schemas.contains(scheme) -> {
                RQES_DOC_RETRIEVAL
            }

            else -> EXTERNAL
        }
    }
}