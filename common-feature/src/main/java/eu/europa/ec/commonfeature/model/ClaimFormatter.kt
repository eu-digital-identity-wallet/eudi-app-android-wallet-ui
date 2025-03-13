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

package eu.europa.ec.commonfeature.model

/**
 * A utility class for formatting claim key-value pairs.
 *
 * This class allows you to build a list of claim key-value pairs and retrieve them as an immutable list.
 *
 * @property items The internal mutable list of [ClaimKeyValue] objects.
 */
data class ClaimFormatter(
    private val items: MutableList<ClaimKeyValue>
) {
    /**
     * Represents a key-value pair used in claims.
     *
     * @property key The key of the claim.
     * @property value The value associated with the claim key.
     */
    data class ClaimKeyValue(
        val key: String,
        val value: String
    )

    fun add(key: String, value: String) {
        items.add(ClaimKeyValue(key, value))
    }

    fun toList(): List<ClaimKeyValue> = items.toList()
}