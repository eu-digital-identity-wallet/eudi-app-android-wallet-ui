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

package eu.europa.ec.uilogic.component.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.europa.ec.resourceslogic.theme.values.textSecondaryDark
import eu.europa.ec.uilogic.component.utils.SPACING_MEDIUM
import eu.europa.ec.uilogic.component.utils.VSpacer
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
    titleStyle: TextStyle = MaterialTheme.typography.headlineSmall.copy(
        color = MaterialTheme.colorScheme.primary
    ),
    subtitle: String? = null,
    subTitleMaxLines: Int = Int.MAX_VALUE,
    subTitleStyle: TextStyle = MaterialTheme.typography.bodyMedium.copy(
        color = MaterialTheme.colorScheme.textSecondaryDark
    ),
    verticalPadding: PaddingValues = PaddingValues(bottom = SPACING_MEDIUM.dp),
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
            horizontalAlignment = Alignment.Start
        ) {
            if (!title.isNullOrEmpty()) {
                Text(
                    text = title,
                    style = titleStyle,
                )
            }
            if (!subtitle.isNullOrEmpty()) {
                if (!title.isNullOrEmpty()) {
                    VSpacer.Small()
                }
                Text(
                    text = subtitle,
                    style = subTitleStyle,
                    maxLines = subTitleMaxLines,
                    overflow = TextOverflow.Ellipsis
                )
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