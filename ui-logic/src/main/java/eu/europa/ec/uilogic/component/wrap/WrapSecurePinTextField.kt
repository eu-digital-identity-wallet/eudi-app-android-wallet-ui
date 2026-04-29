/*
 * Copyright (c) 2026 European Commission
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

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicSecureTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.TextFieldDecorator
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.foundation.text.input.delete
import androidx.compose.foundation.text.input.placeCursorAtEnd
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import eu.europa.ec.businesslogic.model.SecurePin
import eu.europa.ec.businesslogic.model.SecurePinImpl
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.EmptyTextToolbar
import eu.europa.ec.uilogic.component.utils.SIZE_SMALL
import eu.europa.ec.uilogic.component.utils.SPACING_SMALL
import eu.europa.ec.uilogic.extension.applyTestTag
import eu.europa.ec.uilogic.util.TestTag
import kotlinx.coroutines.flow.distinctUntilChanged

@Stable
class SecurePinTextFieldState internal constructor(
    val expectedPinLength: Int,
    internal val textFieldState: TextFieldState
) {
    val currentLength: Int
        get() = textFieldState.text.length

    val isComplete: Boolean
        get() = currentLength == expectedPinLength

    fun toSecurePinAndClear(): SecurePin {
        check(isComplete) {
            "Expected $expectedPinLength PIN characters, but found $currentLength."
        }
        val pin = SecurePinImpl(textFieldState.text)
        clear()
        return pin
    }

    fun clear() {
        textFieldState.edit {
            if (length > 0) {
                delete(0, length)
            }
            placeCursorAtEnd()
        }
    }
}

@Composable
fun rememberSecurePinTextFieldState(
    expectedPinLength: Int
): SecurePinTextFieldState {
    val textFieldState = rememberTextFieldState()
    return remember(expectedPinLength, textFieldState) {
        SecurePinTextFieldState(
            expectedPinLength = expectedPinLength,
            textFieldState = textFieldState
        )
    }
}

@Composable
fun WrapSecurePinTextField(
    modifier: Modifier = Modifier,
    state: SecurePinTextFieldState,
    onPinLengthChanged: (length: Int) -> Unit,
    onPinComplete: ((pin: SecurePin) -> Unit)? = null,
    hasError: Boolean = false,
    errorMessage: String? = null,
    pinWidth: Dp? = null,
    clearCode: Boolean = false,
    focusOnCreate: Boolean = false,
    shouldHideKeyboardOnCompletion: Boolean = false,
    enabled: Boolean = true
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val inputTransformation = remember(state.expectedPinLength) {
        NumericPinInputTransformation(state.expectedPinLength)
    }

    LaunchedEffect(state, onPinComplete, shouldHideKeyboardOnCompletion) {
        snapshotFlow { state.currentLength }
            .distinctUntilChanged()
            .collect { length ->
                onPinLengthChanged(length)
                if (length == state.expectedPinLength && onPinComplete != null) {
                    if (shouldHideKeyboardOnCompletion) {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    }
                    onPinComplete(state.toSecurePinAndClear())
                }
            }
    }

    LaunchedEffect(clearCode) {
        if (clearCode) {
            state.clear()
            onPinLengthChanged(0)
            if (focusOnCreate) {
                focusRequester.requestFocus()
            }
        }
    }

    LaunchedEffect(focusOnCreate) {
        if (focusOnCreate) {
            focusRequester.requestFocus()
        }
    }

    DisposableEffect(state) {
        onDispose {
            state.clear()
        }
    }

    CompositionLocalProvider(
        LocalTextToolbar provides EmptyTextToolbar,
        LocalTextSelectionColors provides TextSelectionColors(
            handleColor = Color.Transparent,
            backgroundColor = Color.Transparent
        )
    ) {
        Column(modifier = modifier) {
            DisableSelection {
                BasicSecureTextField(
                    state = state.textFieldState,
                    modifier = Modifier.focusRequester(focusRequester),
                    inputTransformation = inputTransformation,
                    textStyle = LocalTextStyle.current.copy(color = Color.Transparent),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Done
                    ),
                    readOnly = !enabled,
                    cursorBrush = SolidColor(Color.Transparent),
                    textObfuscationMode = TextObfuscationMode.Hidden,
                    decorator = TextFieldDecorator { innerTextField ->
                        Box {
                            SecurePinCellsRow(
                                length = state.expectedPinLength,
                                filledLength = state.currentLength,
                                hasError = hasError,
                                pinWidth = pinWidth
                            )
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .alpha(0f)
                            ) {
                                innerTextField()
                            }
                        }
                    }
                )
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

@Composable
private fun SecurePinCellsRow(
    length: Int,
    filledLength: Int,
    hasError: Boolean,
    pinWidth: Dp?
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(SPACING_SMALL.dp)
    ) {
        repeat(length) { index ->
            SecurePinCell(
                modifier = Modifier.applyTestTag(TestTag.pinTextField(index)),
                isFilled = index < filledLength,
                hasError = hasError,
                pinWidth = pinWidth
            )
        }
    }
}

@Composable
private fun RowScope.SecurePinCell(
    modifier: Modifier,
    isFilled: Boolean,
    hasError: Boolean,
    pinWidth: Dp?
) {
    val borderColor = when {
        hasError -> MaterialTheme.colorScheme.error
        isFilled -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    Box(
        modifier = modifier
            .then(pinWidth?.let { dp ->
                Modifier.width(dp)
            } ?: Modifier
                .weight(1f)
                .wrapContentSize()
            )
            .defaultMinSize(
                minWidth = OutlinedTextFieldDefaults.MinWidth,
                minHeight = OutlinedTextFieldDefaults.MinHeight,
            )
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(SIZE_SMALL.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isFilled) {
            Text(
                text = "\u2022",
                style = LocalTextStyle.current.copy(
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    }
}

private class NumericPinInputTransformation(
    private val maxLength: Int
) : InputTransformation {
    override fun TextFieldBuffer.transformInput() {
        val proposed = asCharSequence()
        if (length > maxLength || proposed.any { it !in '0'..'9' }) {
            revertAllChanges()
            return
        }
        placeCursorAtEnd()
    }
}

/**
 * Preview composable of [WrapSecurePinTextField].
 */
@ThemeModePreviews
@Composable
private fun PreviewWrapSecurePinTextField() {
    PreviewTheme {

        val pinInputState = rememberSecurePinTextFieldState(
            expectedPinLength = 4
        )

        WrapSecurePinTextField(
            modifier = Modifier.wrapContentSize(),
            state = pinInputState,
            onPinComplete = {},
            pinWidth = 42.dp,
            onPinLengthChanged = {}
        )
    }
}