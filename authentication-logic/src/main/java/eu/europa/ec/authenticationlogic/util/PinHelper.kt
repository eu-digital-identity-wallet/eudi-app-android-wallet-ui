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

package eu.europa.ec.authenticationlogic.util

import eu.europa.ec.authenticationlogic.secure.SecurePin

/**
 * Whether [pin] is easy to guess: one digit repeated, such as 000000; an ascending or descending
 * run, such as 123456 or 654321; or one of [commonPins]. The PIN is read in place and not copied.
 */
fun isWeakPin(pin: SecurePin, commonPins: Set<String>): Boolean =
    pin.readChars { chars -> isWeakPin(chars, commonPins) }

internal fun isWeakPin(chars: CharArray, commonPins: Set<String>): Boolean {
    if (chars.isEmpty()) return false
    val repeated = chars.all { it == chars[0] }
    val ascending = (1 until chars.size).all { chars[it] - chars[it - 1] == 1 }
    val descending = (1 until chars.size).all { chars[it] - chars[it - 1] == -1 }
    val common = commonPins.any { candidate ->
        candidate.length == chars.size && candidate.indices.all { candidate[it] == chars[it] }
    }
    return repeated || ascending || descending || common
}