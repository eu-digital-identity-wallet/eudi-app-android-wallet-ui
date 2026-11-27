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

package eu.europa.ec.dashboardfeature.ui.documents.list

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.LayoutBoundsHolder
import androidx.compose.ui.layout.onVisibilityChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import eu.europa.ec.dashboardfeature.ui.documents.list.model.DocumentUi
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.theme.values.success
import eu.europa.ec.uilogic.component.ListItemMainContentDataUi
import eu.europa.ec.uilogic.component.wrap.WrapListItem
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

private const val HIGHLIGHT_FADE_IN_MILLIS = 150
private val HIGHLIGHT_HOLD = 700.milliseconds
private const val HIGHLIGHT_FADE_OUT_MILLIS = 450
private const val HIGHLIGHT_TINT_FRACTION = 0.20f

/** A grouped document row. Its category can be composed while this row is outside the list viewport. */
@Composable
internal fun DocumentRow(
    document: DocumentUi,
    shape: Shape,
    listViewport: LayoutBoundsHolder,
    canShowHighlights: Boolean,
    awaitingHighlight: Boolean,
    onHighlightShown: () -> Unit,
    onClick: () -> Unit,
) {
    val item = document.uiData
    // A placed row can become eligible after a refresh or sheet closure without moving.
    var isRowVisible by remember(item.itemId) { mutableStateOf(false) }
    var isHighlighting by remember(item.itemId) { mutableStateOf(false) }
    val showHighlight = isHighlighting ||
            (canShowHighlights && awaitingHighlight && isRowVisible && !document.isDeferred)

    val colors = if (showHighlight) {
        val tint = remember { Animatable(0f) }
        LaunchedEffect(Unit) {
            // Acknowledge at display time. Consuming the ID leaves this row's animation running.
            isHighlighting = true
            onHighlightShown()
            tint.animateTo(HIGHLIGHT_TINT_FRACTION, tween(HIGHLIGHT_FADE_IN_MILLIS))
            // With system animations disabled the fades snap, while the hold still shows the tint.
            delay(HIGHLIGHT_HOLD)
            tint.animateTo(0f, tween(HIGHLIGHT_FADE_OUT_MILLIS))
            isHighlighting = false
        }
        CardDefaults.cardColors(
            containerColor = lerp(
                MaterialTheme.colorScheme.surfaceContainer,
                MaterialTheme.colorScheme.success,
                tint.value,
            ),
            contentColor = MaterialTheme.colorScheme.onSurface,
        )
    } else null

    val name = (item.mainContentData as? ListItemMainContentDataUi.Text)?.text
    val announcementModifier = if (showHighlight && name != null) {
        val message = stringResource(R.string.documents_screen_deferred_document_issued, name)
        Modifier.semantics {
            liveRegion = LiveRegionMode.Polite
            stateDescription = message
        }
    } else Modifier

    WrapListItem(
        modifier = Modifier
            .fillMaxWidth()
            .onVisibilityChanged(
                minFractionVisible = 0.5f,
                viewportBounds = listViewport
            ) { visible ->
                isRowVisible = visible
            }
            .then(announcementModifier),
        item = item,
        onItemClick = { onClick() },
        shape = shape,
        colors = colors,
    )
}