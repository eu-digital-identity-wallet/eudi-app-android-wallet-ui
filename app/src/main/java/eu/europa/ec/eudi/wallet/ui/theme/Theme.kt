/*
 * Copyright (c) 2023 European Commission
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.europa.ec.eudi.wallet.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = darkColorScheme(
    primary = ec_blue,
    onPrimary = white,
    primaryContainer = ec_yellow,
    onPrimaryContainer = white,
    secondaryContainer = ec_blue_25,
    onSecondaryContainer = ec_gray,
    surfaceVariant = ec_blue_50,
    onSurfaceVariant = ec_gray,
    onSurface = ec_gray
)

private val DarkColorScheme = lightColorScheme(
    primary = ec_blue,
    onPrimary = white,
    primaryContainer = ec_yellow,
    onPrimaryContainer = white,
    secondaryContainer = ec_blue_25,
    onSecondaryContainer = ec_gray,
    surfaceVariant = ec_blue_50,
    onSurfaceVariant = ec_gray,
    onSurface = ec_gray
)

@Composable
fun WalletUIAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}