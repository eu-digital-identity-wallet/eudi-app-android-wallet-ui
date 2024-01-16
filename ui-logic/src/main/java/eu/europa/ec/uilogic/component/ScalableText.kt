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

package eu.europa.ec.uilogic.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews

@Composable
fun ScalableText(
    text: String,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
    maxLines: Int = 1,
) {
    var style by remember { mutableStateOf(textStyle) }
    var readyToDraw by remember { mutableStateOf(false) }

    Text(
        modifier = modifier.then(
            Modifier
                .drawWithContent {
                    if (readyToDraw) {
                        drawContent()
                    }
                }
        ),
        text = text,
        style = style,
        maxLines = maxLines,
        onTextLayout = { textLayoutResult ->
            if (!readyToDraw && textLayoutResult.hasVisualOverflow) {
                style = style.copy(fontSize = style.fontSize * 0.9)
            } else {
                readyToDraw = true
            }
        }
    )
}

/**
 * This particular preview,
 * due to the fact that [ScalableText] is a stateful composable,
 * should be run in "Interactive Mode" to see it in action.
 * */
@ThemeModePreviews
@Composable
private fun ScalableTextPreview() {
    PreviewTheme {
        val text = "The second text must scale down and stay in one line."
        val textStyle = MaterialTheme.typography.titleMedium.copy(color = Color.Black)
        val screenWidth = 200.dp

        Column(
            modifier = Modifier
                .width(screenWidth)
        ) {
            Text(
                modifier = Modifier
                    .wrapContentHeight(),
                text = text,
                style = textStyle
            )
            ScalableText(
                modifier = Modifier
                    .wrapContentHeight(),
                text = text,
                textStyle = textStyle
            )
        }
    }
}