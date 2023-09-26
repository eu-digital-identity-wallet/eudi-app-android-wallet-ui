/*
 *
 *  * Copyright (c) 2023 European Commission
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package eu.europa.ec.resourceslogic.theme.templates.structures

import androidx.compose.ui.text.style.TextAlign

sealed class ThemeTextAlign {
    data object Left : ThemeTextAlign()
    data object Right : ThemeTextAlign()
    data object Center : ThemeTextAlign()
    data object Justify : ThemeTextAlign()
    data object Start : ThemeTextAlign()
    data object End : ThemeTextAlign()

    companion object {
        fun ThemeTextAlign.toTextAlign(): TextAlign = when (this) {
            Left -> TextAlign.Left
            Right -> TextAlign.Right
            Center -> TextAlign.Center
            Justify -> TextAlign.Justify
            Start -> TextAlign.Start
            End -> TextAlign.End
        }
    }
}