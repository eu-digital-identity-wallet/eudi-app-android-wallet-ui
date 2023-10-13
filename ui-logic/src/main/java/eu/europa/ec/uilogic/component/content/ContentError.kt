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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.uilogic.component.wrap.WrapPrimaryButton

@Composable
internal fun ContentError(config: ContentErrorConfig, paddingValues: PaddingValues) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(paddingValues),
    ) {
        ContentTitle(
            title = config.errorTitle ?: stringResource(
                id = R.string.generic_error_message
            ),
            subtitle = config.errorSubTitle ?: stringResource(
                id = R.string.generic_error_retry
            ),
            subTitleMaxLines = 10
        )

        Spacer(modifier = Modifier.weight(1f))

        config.onRetry?.let { callback ->
            WrapPrimaryButton(
                onClick = {
                    callback()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(id = R.string.generic_error_button_retry)
                )
            }
        }
    }
}

data class ContentErrorConfig(
    val errorTitle: String? = null,
    val errorSubTitle: String? = null,
    val onCancel: () -> Unit,
    val onRetry: (() -> Unit)? = null
)

/**
 * Preview composable of [ContentError].
 */
@Preview
@Composable
fun PreviewContentErrorScreen() {
    ContentError(config = ContentErrorConfig(onCancel = {}), paddingValues = PaddingValues(16.dp))
}