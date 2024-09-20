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

package eu.europa.ec.uilogic.component.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.europa.ec.businesslogic.util.safeLet
import eu.europa.ec.resourceslogic.theme.values.textPrimaryDark
import eu.europa.ec.resourceslogic.theme.values.textSecondaryDark
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.ClickableText
import eu.europa.ec.uilogic.component.utils.SIZE_SMALL
import eu.europa.ec.uilogic.component.utils.SPACING_MEDIUM
import eu.europa.ec.uilogic.component.utils.VSpacer
import eu.europa.ec.uilogic.component.wrap.WrapIcon
import eu.europa.ec.uilogic.extension.clickableNoRipple
import eu.europa.ec.uilogic.extension.throttledClickable

/**
 * Generates a composable that contains the title (if valid) and the subtitle
 * (if valid) for this screen.
 *
 * @param title    Title string to use as content screen title. Set to `null`
 * to hide title.
 * @param subtitle Subtitle string to use as content screen title. Set to `null`
 * to hide title.
 */
@Composable
fun ContentTitle(
    title: String? = null,
    titleWithBadge: TitleWithBadge? = null,
    onTitleWithBadgeClick: (() -> Unit)? = null,
    titleStyle: TextStyle = MaterialTheme.typography.headlineSmall.copy(
        color = MaterialTheme.colorScheme.textPrimaryDark
    ),
    subtitle: String? = null,
    clickableSubtitle: String? = null,
    onSubtitleClick: (() -> Unit)? = null,
    subTitleMaxLines: Int = Int.MAX_VALUE,
    subTitleStyle: TextStyle = MaterialTheme.typography.bodyMedium.copy(
        color = MaterialTheme.colorScheme.textSecondaryDark
    ),
    verticalPadding: PaddingValues = PaddingValues(bottom = SPACING_MEDIUM.dp),
    subtitleTrailingContent: (@Composable RowScope.() -> Unit)? = null,
    trailingLabel: String? = null,
    trailingAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .wrapContentHeight()
            .padding(verticalPadding)
            .then(
                if (trailingLabel != null) {
                    Modifier.fillMaxWidth()
                } else {
                    Modifier.wrapContentWidth()
                }
            ),
        horizontalArrangement = if (trailingLabel != null) {
            Arrangement.SpaceBetween
        } else {
            Arrangement.Start
        },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(SIZE_SMALL.dp)
        ) {
            if (titleWithBadge != null) {
                val inlineContentMap = mapOf(
                    "badgeIconId" to InlineTextContent(
                        Placeholder(20.sp, 20.sp, PlaceholderVerticalAlign.TextCenter)
                    ) {
                        WrapIcon(
                            iconData = AppIcons.Verified,
                            customTint = Color.Green
                        )
                    }
                )

                Text(
                    modifier = onTitleWithBadgeClick?.let {
                        Modifier.clickableNoRipple(
                            onClick = it
                        )
                    } ?: Modifier,
                    text = titleWithBadge.annotatedString,
                    style = titleStyle,
                    inlineContent = inlineContentMap,
                )
            } else if (!title.isNullOrEmpty()) {
                Text(
                    text = title,
                    style = titleStyle,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (!subtitle.isNullOrEmpty()) {
                    if (!title.isNullOrEmpty()) {
                        VSpacer.Small()
                    }

                    safeLet(
                        clickableSubtitle,
                        onSubtitleClick
                    ) { clickableSubtitle, onSubtitleClick ->
                        val annotatedSubtitle = buildAnnotatedString {
                            // Plain, non-clickable text.
                            append(subtitle)

                            // Clickable part of subtitle.
                            withStyle(
                                style = SpanStyle(
                                    fontStyle = subTitleStyle.fontStyle,
                                    color = MaterialTheme.colorScheme.textPrimaryDark
                                )
                            ) {
                                pushStringAnnotation(
                                    tag = clickableSubtitle,
                                    annotation = clickableSubtitle
                                )
                                append(clickableSubtitle)
                            }
                        }

                        ClickableText(
                            modifier = Modifier
                                .weight(1f),
                            text = annotatedSubtitle,
                            onClick = { offset ->
                                annotatedSubtitle.getStringAnnotations(offset, offset)
                                    .firstOrNull()?.let {
                                        onSubtitleClick()
                                    }
                            },
                            style = subTitleStyle,
                            maxLines = subTitleMaxLines,
                            overflow = TextOverflow.Ellipsis
                        )
                    } ?: Text(
                        modifier = Modifier.weight(1f),
                        text = subtitle,
                        style = subTitleStyle,
                        maxLines = subTitleMaxLines,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                subtitleTrailingContent?.invoke(this)
            }
        }
        trailingLabel?.let {
            Text(
                modifier = Modifier
                    .throttledClickable {
                        trailingAction?.invoke()
                    },
                text = trailingLabel,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

data class TitleWithBadge(
    private val textBeforeBadge: String? = null,
    private val textAfterBadge: String? = null,
    val isTrusted: Boolean
) {
    val annotatedString = buildAnnotatedString {
        if (!textBeforeBadge.isNullOrEmpty()) {
            append(textBeforeBadge)
        }
        if (isTrusted) {
            append(" ")
            appendInlineContent(id = "badgeIconId")
        }
        if (!textAfterBadge.isNullOrEmpty()) {
            append(textAfterBadge)
        }
    }

    val plainText: String
        get() = buildString {
            if (!textBeforeBadge.isNullOrEmpty()) {
                append(textBeforeBadge)
            }
            if (!textAfterBadge.isNullOrEmpty()) {
                append(textAfterBadge)
            }
        }
}