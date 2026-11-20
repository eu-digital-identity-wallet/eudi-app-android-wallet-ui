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

package eu.europa.ec.corelogic.extension

import eu.europa.ec.corelogic.config.ReaderAuthenticationEnforcement
import eu.europa.ec.eudi.iso18013.transfer.response.RequestProcessor
import eu.europa.ec.eudi.wallet.trust.ReaderAuthenticationConfigBuilder

/**
 * Configures Wallet Core's reader authentication with [enforcement], so that the presentation flow
 * and Wallet Core apply the same rule.
 *
 * @param enforcement the enforcement from `WalletCoreConfig.readerAuthenticationEnforcement`.
 */
internal fun ReaderAuthenticationConfigBuilder.setEnforcement(
    enforcement: ReaderAuthenticationEnforcement,
) {
    when (enforcement) {
        ReaderAuthenticationEnforcement.DO_NOT_ENFORCE -> doNotEnforce()
        ReaderAuthenticationEnforcement.ENFORCE_IF_PRESENT -> enforceIfPresent()
        ReaderAuthenticationEnforcement.ALWAYS_REQUIRE -> alwaysRequire()
    }
}

/**
 * Whether Wallet Core would answer this request with no data under [enforcement]: an empty mdoc
 * response, or a refused OpenID4VP response, whatever the user shares. It applies Wallet Core's
 * own rule, on every transport: a request is trusted when its trust metadata is set, and carries
 * reader authentication when it names a requester identity.
 *
 * @param enforcement the configured reader authentication enforcement.
 * @return true when the request must be refused before the consent screen.
 */
internal fun RequestProcessor.ProcessedRequest.Success.isRefusedByReaderAuthentication(
    enforcement: ReaderAuthenticationEnforcement,
): Boolean {
    val isReaderTrusted = trustMetadata != null
    val hasReaderAuthentication = requester.requesterIdentities.isNotEmpty()

    return when (enforcement) {
        ReaderAuthenticationEnforcement.DO_NOT_ENFORCE -> false
        ReaderAuthenticationEnforcement.ENFORCE_IF_PRESENT ->
            hasReaderAuthentication && !isReaderTrusted

        ReaderAuthenticationEnforcement.ALWAYS_REQUIRE -> !isReaderTrusted
    }
}
