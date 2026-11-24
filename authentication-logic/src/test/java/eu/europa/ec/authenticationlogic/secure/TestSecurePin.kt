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

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class TestSecurePin {

    @Test
    fun `Given a PIN, When getAndClear is called, Then the characters are returned once`() {
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
    fun `Given a PIN, When getAndClearAsString is called, Then the string is returned once`() {
        // Given
        val mockedPin = "123456"
        val pin = SecurePinImpl(mockedPin)

        // When
        val data = pin.getAndClearAsString()

        // Then
        assertEquals(mockedPin, data)
        assertTrue(pin.isCleared)
        assertThrows(IllegalStateException::class.java) {
            pin.getAndClearAsString()
        }
    }

    @Test
    fun `Given a PIN that was read, When the data is closed, Then its length is zero and it cannot be used`() {
        // Given
        val pin = SecurePinImpl("1234")

        // When
        val data = pin.getAndClear()
        data.close()

        // Then
        assertEquals(0, data.length)
        assertThrows(IllegalStateException::class.java) {
            data.useChars {}
        }
    }

    @Test
    fun `Given two equal PINs and a different one, When they are compared, Then the result is by content and no PIN is cleared`() {
        // Given
        val pinA = SecurePinImpl("1234")
        val pinB = SecurePinImpl("1234")
        val pinC = SecurePinImpl("4321")

        // When
        val equalPins = pinA.contentEquals(pinB)
        val differentPins = pinA.contentEquals(pinC)

        // Then
        assertTrue(equalPins)
        assertFalse(differentPins)
        assertFalse(pinA.isCleared)
        assertFalse(pinB.isCleared)
        assertFalse(pinC.isCleared)

        pinA.close()
        pinB.close()
        pinC.close()
    }

    @Test
    fun `Given PINs with the same value, When they are printed and compared, Then the content is not exposed`() {
        // Given
        val pin = SecurePinImpl("987654")
        val sameValuePin = SecurePinImpl("987654")

        // When
        val pinText = pin.toString()

        // Then
        assertFalse(pinText.contains("987654"))
        assertNotEquals(pin, sameValuePin)

        // When
        val data = pin.getAndClear()
        val dataText = data.toString()

        // Then
        assertFalse(dataText.contains("987654"))

        data.close()
        pin.close()
        sameValuePin.close()
    }

    @Test
    fun `Given a PIN, When readChars is called, Then the characters are given and the PIN is not cleared`() {
        // Given
        val pin = SecurePinImpl("135792")

        // When
        val matches = pin.readChars { it.contentEquals("135792".toCharArray()) }

        // Then
        assertTrue(matches)
        assertFalse(pin.isCleared)
        pin.close()
    }

    @Test
    fun `Given a cleared PIN, When readChars is called, Then it fails`() {
        // Given
        val pin = SecurePinImpl("135792")
        pin.close()

        // Then
        assertThrows(IllegalStateException::class.java) {
            pin.readChars { }
        }
    }
}