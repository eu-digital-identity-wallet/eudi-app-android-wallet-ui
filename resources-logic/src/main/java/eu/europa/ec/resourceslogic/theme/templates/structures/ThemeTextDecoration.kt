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

import androidx.compose.ui.text.style.TextDecoration

sealed class ThemeTextDecoration {
    data object None : ThemeTextDecoration()
    data object Underline : ThemeTextDecoration()
    data object LineThrough : ThemeTextDecoration()

    companion object {
        fun ThemeTextDecoration.toTextDecoration(): TextDecoration = when (this) {
            None -> TextDecoration.None
            Underline -> TextDecoration.Underline
            LineThrough -> TextDecoration.LineThrough
        }
    }
}