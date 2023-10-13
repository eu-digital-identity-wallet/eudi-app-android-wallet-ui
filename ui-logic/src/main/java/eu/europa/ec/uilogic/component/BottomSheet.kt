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

package eu.europa.ec.uilogic.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import eu.europa.ec.resourceslogic.theme.values.textSecondaryDark
import eu.europa.ec.uilogic.component.utils.VSpacer
import eu.europa.ec.uilogic.component.wrap.WrapPrimaryButton
import eu.europa.ec.uilogic.component.wrap.WrapSecondaryButton

@Composable
fun SheetContent(title: String, bodyContent: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .wrapContentHeight()
            .background(color = MaterialTheme.colorScheme.background)
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall.copy(
                color = MaterialTheme.colorScheme.primary
            )
        )
        VSpacer.Small()
        bodyContent()
    }
}

@Composable
fun DialogBottomSheet(
    title: String,
    message: String,
    positiveButtonText: String? = null,
    negativeButtonText: String? = null,
    onPositiveClick: () -> Unit? = {},
    onNegativeClick: () -> Unit? = {}
) {
    SheetContent(title = title) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.textSecondaryDark
            )
        )
        VSpacer.Large()
        positiveButtonText?.let {
            WrapPrimaryButton(
                onClick = { onPositiveClick.invoke() },
                modifier = Modifier.fillMaxWidth(),
                isEnabled = true
            ) {
                Text(
                    text = positiveButtonText
                )
            }
        }
        VSpacer.Medium()
        negativeButtonText?.let {
            WrapSecondaryButton(
                onClick = { onNegativeClick.invoke() },
                modifier = Modifier.fillMaxWidth(),
                isEnabled = true
            ) {
                Text(
                    text = negativeButtonText
                )
            }
        }
    }
}

@Composable
@Preview
fun DialogBottomSheetPreview() {
    DialogBottomSheet(
        title = "Title",
        message = "Message",
        positiveButtonText = "OK",
        negativeButtonText = "Cancel"
    )
}