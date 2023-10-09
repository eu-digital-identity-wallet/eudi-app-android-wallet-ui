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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import eu.europa.ec.uilogic.extension.clickableNoRipple
import eu.europa.ec.uilogic.extension.throttledClickable

@Composable
fun WrapTextField(
    hasNoRipple: Boolean = true,
    modifier: Modifier,
    value: String,
    onValueChange: ((String) -> Unit)? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    onClick: (() -> Unit)? = null,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    errorMsg: String = "",
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    onKeyEvent: ((KeyEvent) -> Boolean)? = null,
    textStyle: TextStyle = LocalTextStyle.current,
) {

    val textFieldModifier = modifier
        .onKeyEvent {
            onKeyEvent?.invoke(it) ?: false
        }
        .then(
            if (hasNoRipple) {
                modifier.clickableNoRipple(
                    enabled = enabled,
                    onClick = onClick ?: {}
                )
            } else {
                Modifier.throttledClickable(enabled = enabled) {
                    onClick?.invoke()
                }
            }
        )

    Column {
        OutlinedTextField(
            modifier = textFieldModifier,
            value = value,
            onValueChange = { newValue ->
                onValueChange?.invoke(newValue)
            },
            enabled = enabled,
            readOnly = readOnly,
            singleLine = singleLine,
            maxLines = maxLines,
            label = label,
            placeholder = placeholder,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon?.let { block -> { block() } },
            isError = isError,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            textStyle = textStyle,
        )
    }

    AnimatedVisibility(
        visible = isError && errorMsg.isNotEmpty(),
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Text(
            text = errorMsg,
            modifier = Modifier.padding(start = 14.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }
}