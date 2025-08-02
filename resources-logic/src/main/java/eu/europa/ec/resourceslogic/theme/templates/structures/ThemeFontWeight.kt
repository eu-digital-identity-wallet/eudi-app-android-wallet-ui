/*
 * Copyright (c) 2025 European Commission
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

package eu.europa.ec.resourceslogic.theme.templates.structures

import androidx.compose.ui.text.font.FontWeight

sealed class ThemeFontWeight {
    data object W100 : ThemeFontWeight()
    data object W200 : ThemeFontWeight()
    data object W300 : ThemeFontWeight()
    data object W400 : ThemeFontWeight()
    data object W500 : ThemeFontWeight()
    data object W600 : ThemeFontWeight()
    data object W700 : ThemeFontWeight()
    data object W800 : ThemeFontWeight()
    data object W900 : ThemeFontWeight()

    companion object {
        fun ThemeFontWeight.toFontWeight(): FontWeight = when (this) {
            W100 -> FontWeight.W100
            W200 -> FontWeight.W200
            W300 -> FontWeight.W300
            W400 -> FontWeight.W400
            W500 -> FontWeight.W500
            W600 -> FontWeight.W600
            W700 -> FontWeight.W700
            W800 -> FontWeight.W800
            W900 -> FontWeight.W900
        }
    }
}