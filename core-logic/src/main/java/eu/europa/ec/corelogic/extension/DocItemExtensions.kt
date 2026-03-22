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

import eu.europa.ec.corelogic.model.ClaimPathDomain
import eu.europa.ec.corelogic.model.ClaimType
import eu.europa.ec.eudi.openid4vp.dcql.ClaimPath
import eu.europa.ec.eudi.openid4vp.dcql.ClaimPathElement
import eu.europa.ec.eudi.iso18013.transfer.response.DocItem
import eu.europa.ec.eudi.iso18013.transfer.response.device.MsoMdocItem
import eu.europa.ec.eudi.wallet.transfer.openId4vp.SdJwtVcItem

fun DocItem.toClaimPath(): ClaimPathDomain {
    return when (this) {
        is MsoMdocItem -> ClaimPathDomain(
            value = listOf(ClaimPathElement.Claim(this.elementIdentifier)),
            type = ClaimType.MsoMdoc(namespace = this.namespace)
        )

        is SdJwtVcItem -> {
            @Suppress("UNCHECKED_CAST")
            val elements = this.path as List<ClaimPathElement>
            ClaimPathDomain(
                value = elements,
                type = ClaimType.SdJwtVc
            )
        }

        else -> ClaimPathDomain(
            value = emptyList(),
            type = ClaimType.Unknown
        )
    }
}