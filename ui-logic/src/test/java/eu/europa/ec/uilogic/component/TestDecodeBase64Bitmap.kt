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

package eu.europa.ec.uilogic.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TestDecodeBase64Bitmap {

    @Test
    fun `Given an image within the maximum dimension, Then it is decoded at full size`() {
        assertEquals(1, sampleSizeFor(width = 600, height = 800))
        assertEquals(1, sampleSizeFor(width = MAX_EMBEDDED_IMAGE_DIMENSION, height = 10))
    }

    @Test
    fun `Given a larger image, Then it is downsampled by a power of two to fit`() {
        assertEquals(2, sampleSizeFor(width = MAX_EMBEDDED_IMAGE_DIMENSION + 1, height = 10))
        assertEquals(4, sampleSizeFor(width = 3000, height = 6000))
        assertEquals(8, sampleSizeFor(width = 16_000, height = 2_000))
    }

    @Test
    fun `Given an image that declares more pixels than the limit, Then it is not decoded`() {
        assertNull(sampleSizeFor(width = 20_000, height = 20_000))
        assertNull(sampleSizeFor(width = Int.MAX_VALUE, height = Int.MAX_VALUE))
    }

    @Test
    fun `Given an unknown size, Then it is not decoded`() {
        assertNull(sampleSizeFor(width = -1, height = -1))
        assertNull(sampleSizeFor(width = 0, height = 100))
    }
}