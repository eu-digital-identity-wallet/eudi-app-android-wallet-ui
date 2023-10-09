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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import eu.europa.ec.resourceslogic.R

@Composable
fun ContentError(
    errorTitle: String = stringResource(id = R.string.generic_error_message),
    errorSubTitle: String = stringResource(id = R.string.generic_error_retry),
    onRetry: (() -> Unit)? = null,
    onCancel: (() -> Unit)? = null
) {
    ContentScreen(
        navigatableAction = onCancel?.let {
            ScreenNavigateAction.CANCELABLE
        } ?: ScreenNavigateAction.NONE,
        onBack = onCancel ?: {},
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(it),
        ) {
            ContentTitle(
                title = errorTitle,
                subtitle = errorSubTitle,
                subTitleMaxLines = 10
            )
            onRetry?.let { callback ->
                WrapPrimaryButton(
                    onClick = {
                        callback()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(id = R.string.generic_error_button_retry),
                        Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * Preview composable of [ContentError].
 */
@Preview
@Composable
fun PreviewContentErrorScreen() {
    ContentError(
        onRetry = {},
        onCancel = {}
    )
}