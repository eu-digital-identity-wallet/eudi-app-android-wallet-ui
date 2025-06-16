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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.SIZE_LARGE
import eu.europa.ec.uilogic.component.utils.SIZE_SMALL
import eu.europa.ec.uilogic.component.utils.SIZE_XX_LARGE
import eu.europa.ec.uilogic.component.utils.SPACING_SMALL
import eu.europa.ec.uilogic.component.wrap.WrapIcon

enum class DualSelectorButton {
    FIRST,
    SECOND
}

data class DualSelectorButtonDataUi(
    val first: String,
    val second: String,
    val selectedButton: DualSelectorButton,
)

@Composable
fun DualSelectorButtons(data: DualSelectorButtonDataUi, onClick: (DualSelectorButton) -> Unit) {

    Row(
        modifier = Modifier
            .height(SIZE_XX_LARGE.dp)
            .fillMaxWidth()
    ) {
        RoundedBorderText(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(
                    color = if (data.selectedButton == DualSelectorButton.FIRST) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        MaterialTheme.colorScheme.surfaceDim
                    },
                    RoundedCornerShape(
                        topStart = SIZE_LARGE.dp,
                        bottomStart = SIZE_LARGE.dp
                    )
                )
                .clip(
                    RoundedCornerShape(
                        topStart = SIZE_LARGE.dp,
                        bottomStart = SIZE_LARGE.dp
                    )
                )
                .clickable { onClick(DualSelectorButton.FIRST) }
                .padding(SIZE_SMALL.dp),
            text = data.first,
            isSelected = data.selectedButton == DualSelectorButton.FIRST
        )
        RoundedBorderText(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(
                    color = if (data.selectedButton == DualSelectorButton.SECOND) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        MaterialTheme.colorScheme.surfaceDim
                    },
                    RoundedCornerShape(
                        topEnd = SIZE_LARGE.dp,
                        bottomEnd = SIZE_LARGE.dp
                    )
                )
                .clip(
                    RoundedCornerShape(
                        topEnd = SIZE_LARGE.dp,
                        bottomEnd = SIZE_LARGE.dp
                    )
                )
                .clickable { onClick(DualSelectorButton.SECOND) }
                .padding(SIZE_SMALL.dp),
            text = data.second,
            isSelected = data.selectedButton == DualSelectorButton.SECOND
        )
    }
}

@Composable
fun RoundedBorderText(
    modifier: Modifier = Modifier,
    text: String,
    isSelected: Boolean,
) {
    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        AnimatedVisibility(isSelected) {
            WrapIcon(
                modifier = Modifier.padding(end = SPACING_SMALL.dp),
                iconData = AppIcons.Check,
                customTint = contentColor
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
            color = contentColor
        )
    }
}

@ThemeModePreviews
@Composable
private fun DualSelectorButtonsPreview() {
    PreviewTheme {
        DualSelectorButtons(
            DualSelectorButtonDataUi(
                first = "offendit",
                second = "principes",
                selectedButton = DualSelectorButton.SECOND
            )
        ) {}
    }
}