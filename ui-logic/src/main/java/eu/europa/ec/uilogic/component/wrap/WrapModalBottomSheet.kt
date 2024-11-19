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

package eu.europa.ec.uilogic.component.wrap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import eu.europa.ec.resourceslogic.theme.values.divider
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.ModalOptionUi
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.SIZE_SMALL
import eu.europa.ec.uilogic.component.utils.SPACING_LARGE
import eu.europa.ec.uilogic.component.utils.SPACING_MEDIUM
import eu.europa.ec.uilogic.component.utils.SPACING_SMALL
import eu.europa.ec.uilogic.component.utils.VSpacer
import eu.europa.ec.uilogic.extension.throttledClickable
import eu.europa.ec.uilogic.mvi.ViewEvent

private val defaultBottomSheetPadding: PaddingValues = PaddingValues(
    all = SPACING_LARGE.dp
)

private val bottomSheetDefaultBackgroundColor: Color
    @Composable get() = MaterialTheme.colorScheme.surfaceContainerLowest

private val bottomSheetDefaultTextColor: Color
    @Composable get() = MaterialTheme.colorScheme.onSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WrapModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState,
    shape: Shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    dragHandle: @Composable (() -> Unit) = { BottomSheetDefaultHandle() },
    sheetContent: @Composable ColumnScope.() -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        shape = shape,
        dragHandle = dragHandle,
        content = sheetContent,
    )
}

@Composable
fun GenericBaseSheetContent(
    title: String,
    bodyContent: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .wrapContentHeight()
            .background(color = bottomSheetDefaultBackgroundColor)
            .fillMaxWidth()
            .padding(defaultBottomSheetPadding)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall.copy(
                color = bottomSheetDefaultTextColor
            )
        )

        VSpacer.Small()
        bodyContent()
    }
}

@Composable
fun GenericBaseSheetContent(
    titleContent: @Composable () -> Unit,
    bodyContent: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .wrapContentHeight()
            .background(color = bottomSheetDefaultBackgroundColor)
            .fillMaxWidth()
            .padding(defaultBottomSheetPadding)
    ) {
        titleContent()
        VSpacer.Large()
        bodyContent()
    }
}

data class BottomSheetTextData(
    val title: String,
    val message: String,
    val positiveButtonText: String? = null,
    val negativeButtonText: String? = null,
)

@Composable
fun DialogBottomSheet(
    textData: BottomSheetTextData,
    onPositiveClick: () -> Unit = {},
    onNegativeClick: () -> Unit = {},
) {
    with(textData) {
        GenericBaseSheetContent(
            title = title,
            bodyContent = {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = bottomSheetDefaultTextColor
                    )
                )
                VSpacer.Large()

                Row {
                    negativeButtonText?.let {
                        WrapSecondaryButton(
                            onClick = { onNegativeClick.invoke() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = negativeButtonText
                            )
                        }
                    }

                    eu.europa.ec.uilogic.component.utils.HSpacer.Small()

                    positiveButtonText?.let {
                        WrapPrimaryButton(
                            onClick = { onPositiveClick.invoke() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = positiveButtonText
                            )
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun <T : ViewEvent> BottomSheetWithOptionsList(
    textData: BottomSheetTextData,
    options: List<ModalOptionUi<T>>,
    onEventSent: (T) -> Unit
) {
    if (options.isNotEmpty()) {
        with(textData) {
            GenericBaseSheetContent(
                title = title,
                bodyContent = {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = bottomSheetDefaultTextColor
                        )
                    )
                    VSpacer.Large()

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.Top,
                        horizontalAlignment = Alignment.Start
                    ) {
                        OptionsList(
                            optionItems = options,
                            itemSelected = onEventSent
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun <T : ViewEvent> OptionsList(
    optionItems: List<ModalOptionUi<T>>,
    itemSelected: (T) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(SPACING_SMALL.dp)
    ) {
        itemsIndexed(optionItems) { index, item ->

            OptionListItem(
                item = item,
                itemSelected = itemSelected
            )

            if (index < optionItems.lastIndex) {
                HorizontalDivider(
                    thickness = 1.dp,
                )
            }
        }
    }
}

@Composable
private fun <T : ViewEvent> OptionListItem(
    item: ModalOptionUi<T>,
    itemSelected: (T) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SIZE_SMALL.dp))
            .background(bottomSheetDefaultBackgroundColor)
            .throttledClickable {
                itemSelected(item.event)
            }
            .padding(
                horizontal = SPACING_SMALL.dp,
                vertical = SPACING_MEDIUM.dp
            ),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = item.title,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = bottomSheetDefaultTextColor
            )
        )

        item.icon?.let {
            WrapIcon(
                modifier = Modifier.wrapContentWidth(),
                iconData = item.icon,
                customTint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun BottomSheetDefaultHandle() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bottomSheetDefaultBackgroundColor)
            .padding(vertical = SPACING_MEDIUM.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        WrapIcon(
            iconData = AppIcons.HandleBar,
            customTint = MaterialTheme.colorScheme.divider
        )
    }
}

@ThemeModePreviews
@Composable
private fun DialogBottomSheetPreview() {
    PreviewTheme {
        DialogBottomSheet(
            textData = BottomSheetTextData(
                title = "Title",
                message = "Message",
                positiveButtonText = "OK",
                negativeButtonText = "Cancel"
            )
        )
    }
}

private data object DummyEventForPreview :
    ViewEvent

@ThemeModePreviews
@Composable
private fun BottomSheetWithOptionsListPreview() {
    PreviewTheme {
        BottomSheetWithOptionsList(
            textData = BottomSheetTextData(
                title = "Title",
                message = "Message"
            ),
            options = buildList {
                addAll(
                    listOf(
                        ModalOptionUi(
                            title = "Option 1",
                            event = DummyEventForPreview
                        ),
                        ModalOptionUi(
                            title = "Option 2",
                            event = DummyEventForPreview
                        ),
                        ModalOptionUi(
                            title = "Option 3",
                            event = DummyEventForPreview
                        ),
                    )
                )
            },
            onEventSent = {}
        )
    }
}

@ThemeModePreviews
@Composable
private fun BottomSheetDefaultHandlePreview() {
    PreviewTheme {
        BottomSheetDefaultHandle()
    }
}