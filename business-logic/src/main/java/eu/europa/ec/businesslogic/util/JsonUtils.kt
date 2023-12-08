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

package eu.europa.ec.businesslogic.util

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Converts given [JSONArray] to a [List]
 */
fun JSONArray.toList(): List<Any> {
    return (0 until this.length()).map {
        this.get(it)
    }
}

/**
 * Attempts to get the given [key] from the [JSONObject]
 * @return its [String] value if it succeeds,
 * empty string if it fails.
 */
fun JSONObject.getStringFromJsonOrEmpty(key: String): String {
    return try {
        this.getString(key)
    } catch (e: JSONException) {
        ""
    }
}