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

package eu.europa.ec.uilogic.component.wrap

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.TextLengthPreviewProvider
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/**
 * Data class representing the configuration for text elements.
 *
 * Fully serializable: [styleKey] identifies the [TextStyle] by name (the
 * [TextStyleKey] enum), and the destination resolves it via [TextStyleKey.toTextStyle]
 * at render time. This means a `TextConfig` can be carried across a navigation
 * argument without losing its style.
 *
 * @property styleKey Optional [TextStyleKey]. `null` means use `LocalTextStyle.current`.
 * @property color Text color. `null` means use `MaterialTheme.colorScheme.onSurface`.
 * @property textAlign Horizontal alignment, defaults to [TextAlign.Start].
 * @property maxLines Maximum number of lines, defaults to 2.
 * @property overflow Overflow handling, defaults to [TextOverflow.Ellipsis].
 */
@Serializable
data class TextConfig(
    val styleKey: TextStyleKey? = null,
    @Contextual val color: Color? = null,
    @Contextual val textAlign: TextAlign = TextAlign.Start,
    val maxLines: Int = 2,
    @Contextual val overflow: TextOverflow = TextOverflow.Ellipsis,
)

@Composable
fun WrapText(
    modifier: Modifier = Modifier,
    text: String,
    textConfig: TextConfig,
) {
    Text(
        modifier = modifier,
        text = text,
        style = textConfig.styleKey?.toTextStyle() ?: LocalTextStyle.current,
        color = textConfig.color ?: MaterialTheme.colorScheme.onSurface,
        textAlign = textConfig.textAlign,
        maxLines = textConfig.maxLines,
        overflow = textConfig.overflow,
    )
}

/**
 * Stable identity for every [TextStyle] used through [TextConfig].
 *
 * Compose [TextStyle] carries deep runtime state (font family resolvers, paint shadow,
 * brush, etc.) that does not round-trip cleanly through serialization. Instead of
 * trying to serialize the whole thing, [TextConfig] carries a [TextStyleKey] and the
 * destination resolves it to a concrete [TextStyle] at render time via
 * [toTextStyle] — picking from the project's `MaterialTheme.typography` scale.
 *
 * The full Material 3 typography scale is present so any future caller has a stable
 * choice without needing to extend this enum. Project-specific variants (e.g.
 * [BodyLargeBold]) are added below the standard scale.
 *
 * **Adding a new style:** add one enum entry here and one branch in [toTextStyle].
 * The compiler enforces the mapping is exhaustive.
 */
@Serializable
enum class TextStyleKey {
    DisplayLarge,
    DisplayMedium,
    DisplaySmall,
    HeadlineLarge,
    HeadlineMedium,
    HeadlineSmall,
    TitleLarge,
    TitleMedium,
    TitleSmall,
    BodyLarge,
    BodyMedium,
    BodySmall,
    LabelLarge,
    LabelMedium,
    LabelSmall,

    /** [BodyLarge] with [FontWeight.W600] applied — used for emphasized body text. */
    BodyLargeBold,
}

/**
 * Resolves a [TextStyleKey] to a live [TextStyle] from the current Material theme.
 * Must be called from a `@Composable` scope because it reads `MaterialTheme.typography`.
 */
@Composable
fun TextStyleKey.toTextStyle(): TextStyle = when (this) {
    TextStyleKey.DisplayLarge -> MaterialTheme.typography.displayLarge
    TextStyleKey.DisplayMedium -> MaterialTheme.typography.displayMedium
    TextStyleKey.DisplaySmall -> MaterialTheme.typography.displaySmall
    TextStyleKey.HeadlineLarge -> MaterialTheme.typography.headlineLarge
    TextStyleKey.HeadlineMedium -> MaterialTheme.typography.headlineMedium
    TextStyleKey.HeadlineSmall -> MaterialTheme.typography.headlineSmall
    TextStyleKey.TitleLarge -> MaterialTheme.typography.titleLarge
    TextStyleKey.TitleMedium -> MaterialTheme.typography.titleMedium
    TextStyleKey.TitleSmall -> MaterialTheme.typography.titleSmall
    TextStyleKey.BodyLarge -> MaterialTheme.typography.bodyLarge
    TextStyleKey.BodyMedium -> MaterialTheme.typography.bodyMedium
    TextStyleKey.BodySmall -> MaterialTheme.typography.bodySmall
    TextStyleKey.LabelLarge -> MaterialTheme.typography.labelLarge
    TextStyleKey.LabelMedium -> MaterialTheme.typography.labelMedium
    TextStyleKey.LabelSmall -> MaterialTheme.typography.labelSmall
    TextStyleKey.BodyLargeBold ->
        MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.W600)
}

@ThemeModePreviews
@Composable
private fun WrapTextConfigPreview(
    @PreviewParameter(TextLengthPreviewProvider::class) text: String
) {
    PreviewTheme {
        WrapText(
            text = text,
            textConfig = TextConfig(
                styleKey = TextStyleKey.BodyLarge,
                maxLines = 1,
            )
        )
    }
}