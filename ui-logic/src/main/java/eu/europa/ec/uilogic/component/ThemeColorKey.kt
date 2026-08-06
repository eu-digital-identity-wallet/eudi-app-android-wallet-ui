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

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import eu.europa.ec.resourceslogic.theme.values.pending
import eu.europa.ec.resourceslogic.theme.values.success
import eu.europa.ec.resourceslogic.theme.values.warning
import kotlinx.serialization.Serializable

/**
 * Identifies a semantic theme color by name for UI models that outlive a theme switch:
 * the model carries the key, and the destination resolves it via [ThemeColorKey.toColor]
 * at render time, so a dark-mode toggle re-resolves it on recomposition.
 *
 * **Adding a new color:** add one enum entry here and one branch in [toColor].
 * The compiler enforces the mapping is exhaustive.
 */
@Serializable
enum class ThemeColorKey {
    Pending,
    Warning,
    Error,
    Success,
    Primary,
}

/**
 * Resolves a [ThemeColorKey] to a live [Color] from the current Material theme.
 * Must be called from a `@Composable` scope because it reads `MaterialTheme.colorScheme`.
 */
@Composable
fun ThemeColorKey.toColor(): Color = when (this) {
    ThemeColorKey.Pending -> MaterialTheme.colorScheme.pending
    ThemeColorKey.Warning -> MaterialTheme.colorScheme.warning
    ThemeColorKey.Error -> MaterialTheme.colorScheme.error
    ThemeColorKey.Success -> MaterialTheme.colorScheme.success
    ThemeColorKey.Primary -> MaterialTheme.colorScheme.primary
}