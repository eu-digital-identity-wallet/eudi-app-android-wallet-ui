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

package eu.europa.ec.corelogic.model

import eu.europa.ec.eudi.wallet.document.ElementIdentifier

data class ClaimPath(val value: List<String>) {
    companion object {
        const val PATH_SEPARATOR = ","

        fun toElementIdentifier(itemId: String): String {
            return itemId
                .split(PATH_SEPARATOR)
                .drop(1)
                .first()
        }

        fun toSdJwtVcPath(itemId: String): List<String> {
            return itemId
                .split(PATH_SEPARATOR)
                .drop(1)
        }
    }

    fun toId(docId: String): String =
        (listOf(docId) + value).joinToString(separator = PATH_SEPARATOR)
}

sealed class DomainClaim {
    abstract val key: ElementIdentifier
    abstract val displayTitle: String
    abstract val path: ClaimPath

    sealed class Claim : DomainClaim() {
        data class Group(
            override val key: ElementIdentifier,
            override val displayTitle: String,
            override val path: ClaimPath,
            val items: List<DomainClaim>,
        ) : Claim()

        data class Primitive(
            override val key: ElementIdentifier,
            override val displayTitle: String,
            override val path: ClaimPath,
            val value: String,
            val isRequired: Boolean,
        ) : Claim()
    }

    data class NotAvailableClaim(
        override val key: ElementIdentifier,
        override val displayTitle: String,
        override val path: ClaimPath,
        val value: String,
    ) : DomainClaim()
}