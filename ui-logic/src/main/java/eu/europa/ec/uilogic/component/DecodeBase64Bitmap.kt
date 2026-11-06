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

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import eu.europa.ec.businesslogic.extension.decodeBase64ToByteArrays
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun rememberBase64DecodedBitmap(base64Image: String): ImageBitmap? {
    var decodedBitmap by remember(base64Image) {
        mutableStateOf<Bitmap?>(null)
    }

    LaunchedEffect(base64Image) {
        decodedBitmap = withContext(Dispatchers.Default) {
            decodeBase64Bitmap(base64Image)
        }
    }

    return remember(decodedBitmap) {
        decodedBitmap?.asImageBitmap()
    }
}

/**
 * The longest side, in pixels, to which an image embedded in a document is decoded. A larger image
 * is downsampled, so that a picture in a credential cannot exhaust the app's memory.
 */
const val MAX_EMBEDDED_IMAGE_DIMENSION: Int = 2048

/** An embedded image that declares more pixels than this is not decoded at all. */
const val MAX_EMBEDDED_IMAGE_PIXELS: Long = 40_000_000

private fun decodeBase64Bitmap(value: String): Bitmap? {
    return value.decodeBase64ToByteArrays().toBitmapOrNull()
}

private fun List<ByteArray>.toBitmapOrNull(): Bitmap? {
    return firstNotNullOfOrNull { decodedBytes -> decodeBoundedBitmap(decodedBytes) }
}

/**
 * Reads the image size from the header first, then decodes the image downsampled per
 * [sampleSizeFor], or not at all when it is too large or not an image.
 */
private fun decodeBoundedBitmap(bytes: ByteArray): Bitmap? = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    val sampleSize = sampleSizeFor(width = bounds.outWidth, height = bounds.outHeight)
        ?: return@runCatching null
    BitmapFactory.decodeByteArray(
        bytes,
        0,
        bytes.size,
        BitmapFactory.Options().apply { inSampleSize = sampleSize }
    )
}.getOrNull()

/**
 * The power-of-two sample size that brings an image of [width] by [height] pixels within
 * [maxDimension] on its longest side, or null when the image must not be decoded: its size is
 * unknown, or it declares more than [maxPixels] pixels.
 */
internal fun sampleSizeFor(
    width: Int,
    height: Int,
    maxDimension: Int = MAX_EMBEDDED_IMAGE_DIMENSION,
    maxPixels: Long = MAX_EMBEDDED_IMAGE_PIXELS,
): Int? {
    if (width <= 0 || height <= 0) return null
    if (width.toLong() * height > maxPixels) return null
    var sampleSize = 1
    while (maxOf(width, height) / sampleSize > maxDimension) {
        sampleSize *= 2
    }
    return sampleSize
}