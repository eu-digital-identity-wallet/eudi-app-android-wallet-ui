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

import eu.europa.ec.authenticationlogic.secure.SecurePinImpl
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TestPinHelper {

    private val commonPins = setOf("147258", "102030")

    @Test
    fun `Given a PIN of one repeated digit, When it is checked, Then it is weak`() {
        assertTrue(isWeakPin(SecurePinImpl("000000"), commonPins))
        assertTrue(isWeakPin(SecurePinImpl("777777"), commonPins))
    }

    @Test
    fun `Given an ascending or descending run, When it is checked, Then it is weak`() {
        assertTrue(isWeakPin(SecurePinImpl("123456"), commonPins))
        assertTrue(isWeakPin(SecurePinImpl("345678"), commonPins))
        assertTrue(isWeakPin(SecurePinImpl("654321"), commonPins))
        assertTrue(isWeakPin(SecurePinImpl("987654"), commonPins))
    }

    @Test
    fun `Given a PIN on the common list, When it is checked, Then it is weak`() {
        assertTrue(isWeakPin(SecurePinImpl("147258"), commonPins))
        assertTrue(isWeakPin(SecurePinImpl("102030"), commonPins))
    }

    @Test
    fun `Given other PINs, When they are checked, Then they are accepted`() {
        assertFalse(isWeakPin(SecurePinImpl("135792"), commonPins))
        assertFalse(isWeakPin(SecurePinImpl("123457"), commonPins))
        assertFalse(isWeakPin(SecurePinImpl("901234"), commonPins))
        assertFalse(isWeakPin(SecurePinImpl("147259"), commonPins))
    }

    @Test
    fun `Given a common PIN of another length, When it is checked, Then it does not match`() {
        assertFalse(isWeakPin(SecurePinImpl("1472"), commonPins))
    }

    @Test
    fun `Given an empty PIN, When it is checked, Then it is not reported as weak`() {
        assertFalse(isWeakPin(CharArray(0), commonPins))
    }

    @Test
    fun `Given a PIN, When it is checked, Then it stays usable`() {
        val pin = SecurePinImpl("135792")

        isWeakPin(pin, commonPins)

        assertFalse(pin.isCleared)
        pin.close()
    }
}