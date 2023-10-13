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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import eu.europa.ec.uilogic.component.utils.EmptyTextToolbar
import eu.europa.ec.uilogic.component.utils.HSpacer
import eu.europa.ec.uilogic.component.utils.SPACING_SMALL

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun WrapPinTextField(
    modifier: Modifier = Modifier.fillMaxWidth(),
    displayCode: String? = null,
    onPinUpdate: (code: String) -> Unit,
    length: Int,
    hasError: Boolean = false,
    errorMessage: String? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    pinWidth: Dp? = null
) {
    // Text field range.
    val fieldsRange = 0 until length

    // Get keyboard controller.
    val keyboardController = LocalSoftwareKeyboardController.current

    // Init list of all digits.
    val textFieldStateList = remember {
        fieldsRange.map {
            mutableStateOf("")
        }
    }

    // Init focus requesters.
    val focusRequesters: List<FocusRequester> = remember {
        fieldsRange.map { FocusRequester() }
    }

    displayCode?.let { otpCode ->
        // Assign each charter from otpCode to the corresponding TextField
        textFieldStateList.forEachIndexed { index, mutableState ->
            mutableState.value = otpCode[index].toString()
        }
        onPinUpdate.invoke(otpCode)
    }

    CompositionLocalProvider(
        LocalTextToolbar provides EmptyTextToolbar
    ) {
        Column(modifier = modifier) {
            Row(
                modifier = Modifier.wrapContentWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (currentTextField in fieldsRange) {
                    WrapTextField(
                        modifier = Modifier
                            .focusRequester(focusRequesters[currentTextField])
                            .then(pinWidth?.let { dp ->
                                Modifier
                                    .width(dp)
                                    .padding(vertical = SPACING_SMALL.dp)
                            } ?: Modifier
                                .weight(1f)
                                .wrapContentSize()),
                        onKeyEvent = {
                            if (it.key == Key.Backspace) {
                                if (textFieldStateList[currentTextField].value.isNotEmpty()) {
                                    textFieldStateList[currentTextField].value = ""
                                } else {
                                    focusRequesters.elementAtOrNull(currentTextField - 1)
                                        ?.requestFocus()
                                }
                                // Notify listener.
                                onPinUpdate.invoke(
                                    textFieldStateList.joinToString(
                                        separator = "",
                                        transform = { textField ->
                                            textField.value
                                        }
                                    )
                                )
                                true
                            } else {
                                false
                            }
                        },
                        value = textFieldStateList[currentTextField].value,
                        textStyle = LocalTextStyle.current.copy(
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        ),
                        visualTransformation = visualTransformation,
                        isError = hasError,
                        onValueChange = { newText: String ->
                            // Set new value, only if it is not blank.
                            if (newText.isNotBlank()) {
                                textFieldStateList[currentTextField].value =
                                    newText.replaceFirst(
                                        textFieldStateList[currentTextField].value,
                                        ""
                                    )

                                // Move to next cell if we are not on the last one.
                                if (currentTextField < length - 1) {
                                    focusRequesters[currentTextField + 1].requestFocus()
                                }

                                // Check if all fields are valid.
                                if (!textFieldStateList.any { textField -> textField.value.isEmpty() }) {
                                    keyboardController?.hide()
                                    focusRequesters.forEach {
                                        it.freeFocus()
                                    }
                                }
                                // Notify listener.
                                onPinUpdate.invoke(
                                    textFieldStateList.joinToString(
                                        separator = "",
                                        transform = { textField ->
                                            textField.value
                                        }
                                    )
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.NumberPassword,
                            imeAction = when (currentTextField < length - 1) {
                                true -> ImeAction.Next
                                false -> ImeAction.Done
                            }
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = {
                                focusRequesters.elementAtOrNull(currentTextField + 1)
                                    ?.requestFocus()
                            }, onDone = {
                                keyboardController?.hide()
                            }
                        )
                    )

                    if (currentTextField != fieldsRange.last) {
                        HSpacer.ExtraSmall()
                    }
                }
            }
            errorMessage?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/**
 * Preview composable of [WrapPinTextField].
 */
@Preview
@Composable
fun PreviewWrapPinTextField() {
    WrapPinTextField(
        modifier = Modifier.wrapContentSize(),
        onPinUpdate = {},
        length = 4,
        visualTransformation = PasswordVisualTransformation(),
        pinWidth = 46.dp
    )
}