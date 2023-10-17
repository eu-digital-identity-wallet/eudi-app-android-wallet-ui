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

package eu.europa.ec.uilogic.component.wrap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import eu.europa.ec.resourceslogic.theme.values.textSecondaryDark
import eu.europa.ec.uilogic.component.utils.VSpacer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WrapModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState,
    shape: Shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    dragHandle: @Composable (() -> Unit)? = null,
    sheetContent: @Composable ColumnScope.() -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        shape = shape,
        dragHandle = dragHandle,
        content = sheetContent
    )
}

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
private fun DialogBottomSheetPreview() {
    DialogBottomSheet(
        title = "Title",
        message = "Message",
        positiveButtonText = "OK",
        negativeButtonText = "Cancel"
    )
}