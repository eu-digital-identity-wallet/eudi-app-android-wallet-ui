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

        fun List<String>.toClaimPath(): ClaimPath {
            return ClaimPath(value = this)
        }

        /**
         * Checks whether this [ClaimPath] is a prefix of another [ClaimPath].
         *
         * A prefix match means that all elements of this path must appear in the same order
         * and at the beginning of the [other] path. This allows partial matches useful in scenarios
         * where the verifier requests a parent claim (e.g., `"address"`) and the wallet contains
         * deeper nested claims (e.g., `"address.street"`, `"address.city"`).
         *
         * Examples:
         * ```
         * ClaimPath(listOf("address")).isPrefixOf(ClaimPath(listOf("address", "city"))) == true
         * ClaimPath(listOf("claim1", "a")).isPrefixOf(ClaimPath(listOf("claim1", "a", "b"))) == true
         * ClaimPath(listOf("name")).isPrefixOf(ClaimPath(listOf("name_birth"))) == false
         * ```
         *
         * @param other The [ClaimPath] to compare against.
         * @return `true` if this path is a prefix of [other]; `false` otherwise.
         */
        fun ClaimPath.isPrefixOf(other: ClaimPath): Boolean {
            return this.value.size <= other.value.size &&
                    this.value.zip(other.value).all { (a, b) -> a == b }
        }
    }

    val joined: String
        get() = value.joinToString(PATH_SEPARATOR)

    fun toId(docId: String): String =
        (listOf(docId) + value).joinToString(separator = PATH_SEPARATOR)
}