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

package eu.europa.ec.commonfeature.ui.request

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import eu.europa.ec.commonfeature.ui.request.model.RequestDataUi
import eu.europa.ec.commonfeature.ui.request.model.RequestDocumentItemUiOld
import eu.europa.ec.commonfeature.ui.request.model.RequiredFieldsItemUi
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.theme.values.warning
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.CardWithIconAndText
import eu.europa.ec.uilogic.component.CheckboxWithContent
import eu.europa.ec.uilogic.component.ErrorInfo
import eu.europa.ec.uilogic.component.InfoTextWithNameAndIconData
import eu.europa.ec.uilogic.component.InfoTextWithNameAndValue
import eu.europa.ec.uilogic.component.InfoTextWithNameAndValueData
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.SPACING_EXTRA_SMALL
import eu.europa.ec.uilogic.component.utils.SPACING_MEDIUM
import eu.europa.ec.uilogic.component.utils.VSpacer
import eu.europa.ec.uilogic.component.wrap.CheckboxData
import eu.europa.ec.uilogic.component.wrap.WrapExpandableCard
import eu.europa.ec.uilogic.component.wrap.WrapIcon

@Composable
fun <T> Request(
    modifier: Modifier = Modifier,
    items: List<RequestDataUi<T>>,
    noData: Boolean,
    isShowingFullUserInfo: Boolean,
    onEventSend: (T) -> Unit,
    listState: LazyListState,
    contentPadding: PaddingValues = PaddingValues(all = 0.dp),
) {
    if (noData) {
        ErrorInfo(
            informativeText = stringResource(id = R.string.request_no_data),
            modifier = modifier
        )
    } else {
        LazyColumn(
            modifier = modifier,
            state = listState,
            verticalArrangement = Arrangement.Top
        ) {

            items(items) { item ->
                when (item) {
                    is RequestDataUi.Divider -> {
                        HorizontalDivider()
                    }

                    is RequestDataUi.Document -> {
                        DocumentCard(
                            cardText = item.documentItemUi.title,
                        )
                    }

                    is RequestDataUi.OptionalField -> {
                        Field(
                            item = item.optionalFieldItemUi.requestDocumentItemUi,
                            showFullDetails = isShowingFullUserInfo,
                            onEventSend = onEventSend,
                        )
                    }

                    is RequestDataUi.RequiredFields -> {
                        RequiredCard(
                            item = item.requiredFieldsItemUi,
                            showFullDetails = isShowingFullUserInfo,
                            onEventSend = onEventSend,
                            contentPadding = contentPadding,
                        )
                    }

                    is RequestDataUi.Space -> {
                        VSpacer.Custom(space = item.space)
                    }
                }
            }
        }
    }
}

@Composable
fun DocumentCard(
    cardText: String,
) {
    CardWithIconAndText(
        text = {
            Text(
                text = cardText,
                style = MaterialTheme.typography.titleSmall,
                color = Color.Black
            )
        },
        icon = {
            WrapIcon(
                modifier = Modifier.size(50.dp),
                iconData = AppIcons.Id,
                customTint = MaterialTheme.colorScheme.primary
            )
        },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondary,
        ),
        contentPadding = PaddingValues(horizontal = SPACING_MEDIUM.dp)
    )
}

@Composable
fun <T> Field(
    item: RequestDocumentItemUiOld<T>,
    showFullDetails: Boolean,
    onEventSend: (T) -> Unit,
) {
    CheckboxWithContent(
        modifier = Modifier.fillMaxWidth(),
        checkboxData = CheckboxData(
            isChecked = item.checked,
            enabled = item.enabled,
            onCheckedChange = {
                item.event?.let { event ->
                    onEventSend(event)
                }
            }
        )
    ) {
        val infoName = item.readableName
        val infoValueStyle = if (item.checked) {
            MaterialTheme.typography.titleMedium
        } else {
            MaterialTheme.typography.bodyLarge
        }

        if (showFullDetails) {
            if (item.keyIsBase64) {
                InfoTextWithNameAndIconData(
                    title = infoName,
                    icon = AppIcons.User,
                    iconModifier = Modifier.size(20.dp)
                )
            } else {
                InfoTextWithNameAndValue(
                    itemData = InfoTextWithNameAndValueData.create(
                        title = infoName,
                        item.value,
                    ),
                    infoValueTextStyle = infoValueStyle
                )
            }
        } else {
            Text(
                text = infoName,
                style = infoValueStyle
            )
        }
    }
}

@Composable
fun <T> RequiredCard(
    item: RequiredFieldsItemUi<T>,
    showFullDetails: Boolean,
    onEventSend: (T) -> Unit,
    contentPadding: PaddingValues
) {
    val requiredFieldsTextStyle = MaterialTheme.typography.titleMedium
    val requiredFieldsTitlePadding = PaddingValues(
        horizontal = contentPadding.calculateStartPadding(LayoutDirection.Ltr) + SPACING_EXTRA_SMALL.dp,
        vertical = contentPadding.calculateBottomPadding()
    )

    if (item.requestDocumentItemsUi.isNotEmpty()) {
        WrapExpandableCard(
            modifier = Modifier.fillMaxWidth(),
            cardCollapsedContent = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.title,
                        style = requiredFieldsTextStyle
                    )
                    val icon = if (item.expanded) {
                        AppIcons.KeyboardArrowUp
                    } else {
                        AppIcons.KeyboardArrowDown
                    }
                    WrapIcon(
                        iconData = icon,
                        customTint = MaterialTheme.colorScheme.primary
                    )
                }
            },
            //cardTitlePadding = requiredFieldsTitlePadding,
            cardExpandedContent = {
                item.requestDocumentItemsUi.forEach { requiredUserIdentificationUi ->
                    Field(
                        item = requiredUserIdentificationUi,
                        showFullDetails = showFullDetails,
                        onEventSend = onEventSend
                    )
                }
            },
            //cardContentPadding = PaddingValues(all = SPACING_SMALL.dp),
            //onCardClick = { onEventSend(item.event) },
            throttleClicks = false,
            isExpanded = item.expanded,
            onExpandedChange = { onEventSend(item.event) },
        )
    }
}

@Composable
fun WarningCard(warningText: String) {
    CardWithIconAndText(
        text = {
            Text(
                text = warningText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        icon = {
            WrapIcon(
                modifier = Modifier.size(32.dp),
                iconData = AppIcons.Warning,
                customTint = MaterialTheme.colorScheme.warning
            )
        },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.warning.copy(alpha = 0.12f)
        ),
        contentPadding = PaddingValues(SPACING_MEDIUM.dp)
    )
}

@ThemeModePreviews
@Composable
private fun IdentificationPartyCardPreview() {
    PreviewTheme {
        DocumentCard(
            cardText = "Warning",
        )
    }
}