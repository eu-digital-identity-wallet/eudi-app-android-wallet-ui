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

package eu.europa.ec.uilogic.component.wrap

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.text.isDigitsOnly
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.EmptyTextToolbar
import eu.europa.ec.uilogic.component.utils.HSpacer
import eu.europa.ec.uilogic.component.utils.OneTimeLaunchedEffect
import eu.europa.ec.uilogic.component.utils.SIZE_SMALL
import eu.europa.ec.uilogic.component.utils.SPACING_SMALL

@Composable
fun WrapPinTextField(
    modifier: Modifier = Modifier.fillMaxWidth(),
    displayCode: String? = null,
    onPinUpdate: (code: String) -> Unit,
    length: Int,
    hasError: Boolean = false,
    errorMessage: String? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    pinWidth: Dp? = null,
    clearCode: Boolean = false,
    focusOnCreate: Boolean = false,
    shouldHideKeyboardOnCompletion: Boolean = false
) {

    fun List<FocusRequester>.requestFocus(index: Int) {
        this.elementAtOrNull(index)?.requestFocus()
    }

    // Text field range.
    val fieldsRange = 0 until length

    // Get keyboard controller.
    val keyboardController = LocalSoftwareKeyboardController.current

    // Get Focus Manager
    val focusManager = LocalFocusManager.current

    // Init list of all digits.
    val textFieldStateList = rememberSaveable {
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

    if (clearCode) {
        textFieldStateList.forEach {
            it.value = ""
            onPinUpdate.invoke("")
        }
        focusRequesters.requestFocus(0)
    }

    CompositionLocalProvider(
        LocalTextToolbar provides EmptyTextToolbar,
        LocalTextSelectionColors provides TextSelectionColors(
            handleColor = Color.Transparent,
            backgroundColor = Color.Transparent
        )
    ) {
        Column(modifier = modifier) {
            Row(
                modifier = Modifier.wrapContentWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (currentTextField in fieldsRange) {
                    DisableSelection {
                        OutlinedTextField(
                            modifier = Modifier
                                .focusRequester(focusRequesters[currentTextField])
                                .then(pinWidth?.let { dp ->
                                    Modifier
                                        .width(dp)
                                        .padding(vertical = SPACING_SMALL.dp)
                                } ?: Modifier
                                    .weight(1f)
                                    .wrapContentSize())
                                .then(
                                    Modifier.onKeyEvent { keyEvent ->
                                        if (keyEvent.key == Key.Backspace) {
                                            if (textFieldStateList[currentTextField].value.isNotEmpty()) {
                                                textFieldStateList[currentTextField].value = ""
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
                                            focusRequesters.requestFocus(currentTextField - 1)
                                            true
                                        } else {
                                            false
                                        }
                                    }
                                ),
                            shape = RoundedCornerShape(SIZE_SMALL.dp),
                            value = textFieldStateList[currentTextField].value,
                            textStyle = LocalTextStyle.current.copy(
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                            ),
                            colors = OutlinedTextFieldDefaults.colors().copy(
                                cursorColor = Color.Transparent,
                                errorCursorColor = Color.Transparent
                            ),
                            visualTransformation = visualTransformation,
                            isError = hasError,
                            singleLine = true,
                            onValueChange = { newText: String ->

                                if (
                                    !newText.isDigitsOnly()
                                    || ((textFieldStateList.all { textField -> textField.value.isEmpty() }
                                            || textFieldStateList.all { textField -> textField.value.isNotEmpty() })
                                            && currentTextField == fieldsRange.last
                                            && newText.isNotEmpty())
                                ) {
                                    return@OutlinedTextField
                                }

                                if (newText != textFieldStateList[currentTextField].value) {
                                    textFieldStateList[currentTextField].value =
                                        newText.replaceFirst(
                                            textFieldStateList[currentTextField].value,
                                            ""
                                        )

                                    // Check if all fields are valid.
                                    if (
                                        !textFieldStateList.any { textField -> textField.value.isEmpty() }
                                        && shouldHideKeyboardOnCompletion
                                    ) {
                                        focusManager.clearFocus()
                                        keyboardController?.hide()
                                    } else if (currentTextField < fieldsRange.last && newText.isNotEmpty()) {
                                        focusRequesters.requestFocus(currentTextField + 1)
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
                                imeAction = when (currentTextField < fieldsRange.last) {
                                    true -> ImeAction.Next
                                    false -> ImeAction.Done
                                }
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = {
                                    focusRequesters.requestFocus(currentTextField + 1)
                                }, onDone = {
                                    keyboardController?.hide()
                                }
                            )
                        )
                    }

                    if (currentTextField != fieldsRange.last) {
                        HSpacer.Small()
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

            OneTimeLaunchedEffect {
                if (focusOnCreate) {
                    focusRequesters.requestFocus(0)
                }
            }
        }
    }
}

/**
 * Preview composable of [WrapPinTextField].
 */
@ThemeModePreviews
@Composable
private fun PreviewWrapPinTextField() {
    PreviewTheme {
        WrapPinTextField(
            modifier = Modifier.wrapContentSize(),
            onPinUpdate = {},
            length = 4,
            visualTransformation = PasswordVisualTransformation(),
            pinWidth = 46.dp,
        )
    }
}