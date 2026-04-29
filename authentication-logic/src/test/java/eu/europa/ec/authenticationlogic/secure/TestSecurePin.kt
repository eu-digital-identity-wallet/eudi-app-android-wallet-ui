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

package eu.europa.ec.authenticationlogic.secure

import org.junit.Assert
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class TestSecurePin {

    @Test
    fun `getAndClear returns characters once`() {

        // Given
        val pin = SecurePinImpl("123456")

        // When
        val data = pin.getAndClear()

        // Then
        data.useChars {
            assertArrayEquals(charArrayOf('1', '2', '3', '4', '5', '6'), it)
        }
        assertTrue(pin.isCleared)
        assertThrows(IllegalStateException::class.java) {
            pin.getAndClear()
        }
        data.close()
    }

    @Test
    fun `getAndClearAsString returns string once`() {

        // Given
        val mockedPin = "123456"
        val pin = SecurePinImpl(mockedPin)

        // When
        val data = pin.getAndClearAsString()

        // Then
        assertEquals(data, mockedPin)
        assertTrue(pin.isCleared)
        assertThrows(IllegalStateException::class.java) {
            pin.getAndClearAsString()
        }
    }

    @Test
    fun `close clears sensitive data`() {

        // Given
        val pin = SecurePinImpl("1234")

        // When
        val data = pin.getAndClear()
        data.close()

        // Then
        Assert.assertEquals(0, data.length)
        assertThrows(IllegalStateException::class.java) {
            data.useChars {}
        }
    }

    @Test
    fun `contentEquals compares without clearing pins`() {

        // Given
        val pinA = SecurePinImpl("1234")
        val pinB = SecurePinImpl("1234")
        val pinC = SecurePinImpl("4321")

        // Then
        assertTrue(pinA.contentEquals(pinB))
        assertFalse(pinA.contentEquals(pinC))
        assertFalse(pinA.isCleared)
        assertFalse(pinB.isCleared)
        assertFalse(pinC.isCleared)

        pinA.close()
        pinB.close()
        pinC.close()
    }

    @Test
    fun `object methods do not expose pin content`() {

        // Given
        val pin = SecurePinImpl("987654")
        val sameValuePin = SecurePinImpl("987654")

        // Then
        assertFalse(pin.toString().contains("987654"))
        assertNotEquals(pin, sameValuePin)

        // Given
        val data = pin.getAndClear()

        // Then
        assertFalse(data.toString().contains("987654"))

        data.close()
        pin.close()
        sameValuePin.close()
    }
}