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
    fun `Given images within the maximum dimension, When the sample size is computed, Then they are decoded at full size`() {
        // When
        val typical = sampleSizeFor(width = 600, height = 800)
        val atMaximum = sampleSizeFor(width = MAX_EMBEDDED_IMAGE_DIMENSION, height = 10)

        // Then
        assertEquals(1, typical)
        assertEquals(1, atMaximum)
    }

    @Test
    fun `Given larger images, When the sample size is computed, Then they are downsampled by a power of two to fit`() {
        // When
        val justOver = sampleSizeFor(width = MAX_EMBEDDED_IMAGE_DIMENSION + 1, height = 10)
        val tall = sampleSizeFor(width = 3000, height = 6000)
        val wide = sampleSizeFor(width = 16_000, height = 2_000)

        // Then
        assertEquals(2, justOver)
        assertEquals(4, tall)
        assertEquals(8, wide)
    }

    @Test
    fun `Given images that declare more pixels than the limit, When the sample size is computed, Then they are not decoded`() {
        // When
        val tooManyPixels = sampleSizeFor(width = 20_000, height = 20_000)
        val maxValues = sampleSizeFor(width = Int.MAX_VALUE, height = Int.MAX_VALUE)

        // Then
        assertNull(tooManyPixels)
        assertNull(maxValues)
    }

    @Test
    fun `Given an unknown size, When the sample size is computed, Then it is not decoded`() {
        // When
        val negative = sampleSizeFor(width = -1, height = -1)
        val zeroWidth = sampleSizeFor(width = 0, height = 100)

        // Then
        assertNull(negative)
        assertNull(zeroWidth)
    }
}