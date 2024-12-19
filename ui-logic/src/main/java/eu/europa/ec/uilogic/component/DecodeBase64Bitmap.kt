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

package eu.europa.ec.uilogic.component

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import eu.europa.ec.businesslogic.extension.decodeFromBase64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun rememberBase64DecodedBitmap(base64Image: String): ImageBitmap? {
    var decodedImage by remember(base64Image) {
        mutableStateOf<Bitmap?>(null)
    }

    LaunchedEffect(decodedImage) {
        if (decodedImage != null) return@LaunchedEffect
        launch(Dispatchers.Default) {
            decodedImage = try {
                val decodedImageByteArray: ByteArray =
                    decodeFromBase64(base64Image, Base64.URL_SAFE)
                BitmapFactory.decodeByteArray(decodedImageByteArray, 0, decodedImageByteArray.size)
            } catch (e: Exception) {
                null
            }
        }
    }

    return remember(decodedImage) {
        decodedImage?.asImageBitmap()
    }
}