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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.utils.HSpacer
import eu.europa.ec.uilogic.component.wrap.CheckboxData
import eu.europa.ec.uilogic.component.wrap.WrapCheckbox

@Composable
fun CheckboxWithContent(
    checkboxData: CheckboxData,
    modifier: Modifier = Modifier,
    checkboxModifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        WrapCheckbox(
            checkboxData = checkboxData,
            modifier = checkboxModifier
        )

        HSpacer.Medium()

        content(this)
    }
}

@Preview
@Composable
private fun CheckboxWithContentPreview() {
    var isChecked by remember {
        mutableStateOf(true)
    }
    val checkBoxData = CheckboxData(
        isChecked = isChecked,
        onCheckedChange = {
            isChecked = it
        },
        enabled = true
    )

    PreviewTheme {
        CheckboxWithContent(
            checkboxData = checkBoxData,
            content = {
                Text(text = "Name")
            }
        )
    }
}