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

package eu.europa.ec.uilogic.navigation.helper

import android.net.Uri
import eu.europa.ec.uilogic.BuildConfig

data class DeepLinkAction(
    val link: Uri,
    val type: DeepLinkType
)

/**
 * @property path The path a link must have, compared exactly; null when any path is accepted.
 */
enum class DeepLinkType(
    val schemas: List<String>,
    val host: String? = null,
    val path: String? = null
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
        host = BuildConfig.RQES_HOST,
        path = BuildConfig.RQES_PATH
    ),
    RQES_DOC_RETRIEVAL(
        schemas = listOf(BuildConfig.RQES_DOC_RETRIEVAL_SCHEME),
        host = BuildConfig.RQES_DOC_RETRIEVAL_HOST
    );

    companion object {

        /**
         * A configured host that matches any host, as the Android manifest treats it. The reference
         * build ships it for [RQES_DOC_RETRIEVAL] so that the demo signing flow works; a production
         * build sets `rqesDocRetrievalHost` to the host of its document-retrieval service.
         */
        const val ANY_HOST = "*"

        /**
         * Whether [host] is the [configured] one, or [configured] is [ANY_HOST]. Hosts are compared
         * without regard to case. A configured host must be exact: the manifest's `*.domain` form
         * is not supported here, so with it no link would match and the wallet would ignore every
         * such link (see [EXTERNAL]).
         */
        internal fun hostMatches(configured: String?, host: String?): Boolean =
            configured == ANY_HOST || (host != null && host.equals(configured, ignoreCase = true))

        /**
         * The flow a link with [scheme], [host] and [path] starts. A link that matches none,
         * although the wallet's manifest accepted it, is [EXTERNAL]. An [RQES] link must also have
         * the path of the signing service's callback, as on iOS, because any app can open the
         * scheme. [rqesDocRetrievalHost] is the configured document-retrieval host; it is a
         * parameter only so that a specific host can be tested.
         */
        fun parse(
            scheme: String,
            host: String? = null,
            path: String? = null,
            rqesDocRetrievalHost: String? = RQES_DOC_RETRIEVAL.host,
        ): DeepLinkType = when {

            OPENID4VP.schemas.contains(scheme) -> {
                OPENID4VP
            }

            CREDENTIAL_OFFER.schemas.contains(scheme) -> {
                CREDENTIAL_OFFER
            }

            ISSUANCE.schemas.contains(scheme) && hostMatches(ISSUANCE.host, host) -> {
                ISSUANCE
            }

            RQES.schemas.contains(scheme) && hostMatches(RQES.host, host) && path == RQES.path -> {
                RQES
            }

            RQES_DOC_RETRIEVAL.schemas.contains(scheme) && hostMatches(
                rqesDocRetrievalHost,
                host
            ) -> {
                RQES_DOC_RETRIEVAL
            }

            else -> EXTERNAL
        }
    }
}