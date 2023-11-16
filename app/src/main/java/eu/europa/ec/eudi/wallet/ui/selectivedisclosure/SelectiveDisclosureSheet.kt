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

package eu.europa.ec.eudi.wallet.ui.selectivedisclosure

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import eu.europa.ec.eudi.wallet.ui.R
import eu.europa.ec.eudi.wallet.ui.theme.WalletUIAppTheme
import eu.europa.ec.eudi.wallet.ui.theme.ec_green_n
import eu.europa.ec.eudi.iso18013.transfer.DocItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectiveDisclosureSheet(
    modifier: Modifier = Modifier,
    title: String,
    isTrustedReader: Boolean = false,
    isSendingInProgress: Boolean = false,
    sheetData: List<SelectiveDisclosureSheetData> = emptyList(),
    onDocItemToggled: (credentialName: String, element: DocItem) -> Unit = { _: String, _: DocItem -> },
    onConfirm: () -> Unit = {},
    onCancel: () -> Unit = {}
) {

    val state = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { false }
    )

    ModalBottomSheet(
        modifier = modifier,
        sheetState = state,
        onDismissRequest = { onCancel() },
        containerColor = if (!isSystemInDarkTheme()) Color.White else Color(0xFF1a1b18)
    ) {
        ConstraintLayout {

            val (mainSheet, SheetActionsButtons) = createRefs()

            Column(modifier = Modifier
                .constrainAs(mainSheet) {
                    bottom.linkTo(SheetActionsButtons.top)
                    top.linkTo(parent.top)
                }
                .padding(vertical = 16.dp)
                .nestedScroll(
                    rememberNestedScrollInteropConnection()
                )) {
                if (isTrustedReader) {
                    TrustedReaderCheck(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )
                }
                Text(
                    text = title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                ) {
                    DocumentElements(sheetData, onDocItemToggled)
                    if (isSendingInProgress) {
                        LoadingIndicator(
                            modifier = Modifier
                                .matchParentSize()
                                .padding(horizontal = 8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer.copy(
                                        alpha = 0.6f
                                    )
                                ),
                        )
                    }
                }
            }
            SheetActions(
                Modifier
                    .constrainAs(SheetActionsButtons) {
                        bottom.linkTo(parent.bottom)
                    }
                    .fillMaxWidth(),
                enabled = !isSendingInProgress,
                onCancel = onCancel,
                onConfirm = onConfirm
            )
        }
    }
}

@Composable
private fun TrustedReaderCheck(
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.Center) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(ec_green_n)
                .size(48.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                modifier = Modifier.size(24.dp),
                imageVector = Icons.Default.Check,
                contentDescription = "",
                tint = Color.White
            )
        }
    }
}

@Composable
private fun DocumentTitle(
    modifier: Modifier = Modifier,
    document: SelectiveDisclosureSheetData
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            textAlign = TextAlign.Center,
            text = document.documentName,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun ChipsRow(
    modifier: Modifier = Modifier,
    left: SelectiveDisclosureSheetData.DocumentItem,
    right: SelectiveDisclosureSheetData.DocumentItem?,
    credentialName: String,
    onDocItemToggled: (credentialName: String, element: DocItem) -> Unit
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        val chipModifier = if (right != null) Modifier.weight(1f) else Modifier
        ElementChip(
            modifier = chipModifier,
            credentialName,
            documentElement = left,
            onDocItemToggled = onDocItemToggled
        )
        right?.let {
            Spacer(modifier = Modifier.width(8.dp))
            ElementChip(
                modifier = chipModifier,
                credentialName,
                documentElement = right,
                onDocItemToggled = onDocItemToggled
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ElementChip(
    modifier: Modifier = Modifier,
    credentialName: String,
    documentElement: SelectiveDisclosureSheetData.DocumentItem,
    onDocItemToggled: (credentialName: String, element: DocItem) -> Unit
) {
    val isPresent = documentElement.isPresent
    var isChecked by remember { mutableStateOf(isPresent) }

    FilterChip(
        modifier = if (isPresent) modifier.clickable { } else modifier,
        selected = isChecked && isPresent,
        onClick = {
            if (isPresent) {
                isChecked = !isChecked
                onDocItemToggled(credentialName, documentElement.requestedItem)
            }
        },
        label = {
            Text(
                text = documentElement.displayName,
                color = if (!isPresent) Color(0xFF707074) else LocalContentColor.current
            )
        },
        leadingIcon = {
            if (isChecked && isPresent) {
                AnimatedVisibility(visible = true) {
                    Icon(
                        imageVector = Icons.Filled.Done,
                        contentDescription = "",
                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                    )
                }
            }
        },
        colors = if (!isPresent) FilterChipDefaults.filterChipColors(
            containerColor = Color(
                0xFFdfdfe0
            )
        ) else FilterChipDefaults.filterChipColors()
    )
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun DocumentElements(
    sheetData: List<SelectiveDisclosureSheetData>,
    onDocItemToggled: (credentialName: String, element: DocItem) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        sheetData.forEach { document ->
            stickyHeader {
                DocumentTitle(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    document = document
                )
            }
            val sortedItems = document.requestedItems.sortedByDescending { it.isPresent }
            val grouped = sortedItems.chunked(2).map { pair ->
                if (pair.size == 1) Pair(pair.first(), null)
                else Pair(pair.first(), pair.last())
            }
            items(grouped.size) { index ->
                val items = grouped[index]
                ChipsRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    left = items.first,
                    right = items.second,
                    document.credentialName,
                    onDocItemToggled = onDocItemToggled
                )
            }
        }
    }
}

@Composable
private fun LoadingIndicator(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun SheetActions(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Button(
            modifier = Modifier.weight(1f),
            enabled = enabled,
            onClick = {
                if (enabled) {
                    onCancel()
                }
            }
        ) {
            Text(text = stringResource(id = android.R.string.cancel))
        }
        Spacer(modifier = Modifier.width(8.dp))
        Button(
            modifier = Modifier.weight(1f),
            enabled = enabled,
            onClick = {
                if (enabled) {
                    onConfirm()
                }
            }
        ) {
            Text(text = stringResource(id = R.string.send))
        }
    }
}

@Composable
@Preview(name = "Default", showBackground = true)
@Preview(name = "Default", showBackground = true, uiMode = UI_MODE_NIGHT_YES)
private fun PreviewSelectiveDisclosureSheet() {
    WalletUIAppTheme {
        SelectiveDisclosureSheet(
            modifier = Modifier.fillMaxSize(),
            title = "Title"
        )
    }
}

@Composable
@Preview(name = "Default With Trusted Reader", showBackground = true)
@Preview(name = "Default With Trusted Reader", showBackground = true, uiMode = UI_MODE_NIGHT_YES)
private fun PreviewSelectiveDisclosureSheetTrustedReader() {
    WalletUIAppTheme {
        SelectiveDisclosureSheet(
            modifier = Modifier.fillMaxSize(),
            title = "Title",
            isTrustedReader = true
        )
    }
}

@Composable
@Preview(name = "Document With Trusted Reader", showBackground = true)
@Preview(name = "Document With Trusted Reader", showBackground = true, uiMode = UI_MODE_NIGHT_YES)
private fun PreviewSelectiveDisclosureSheetWithDocumentAndTrustedReader() {
    WalletUIAppTheme {
        SelectiveDisclosureSheet(
            modifier = Modifier.fillMaxSize(),
            title = "Trusted verifier 'Reader' is requesting the following information",
            isTrustedReader = true,
            sheetData = listOf(
                SelectiveDisclosureSheetData(
                    credentialName = "Credential Name",
                    documentName = "Driving Licence  |  mDL",
                    requestedItems = (1..11).map {
                        SelectiveDisclosureSheetData.DocumentItem(
                            "Property $it",
                            DocItem("$it", "namespace"),
                            isPresent = true
                        )
                    }
                )
            )
        )
    }
}

@Composable
@Preview(name = "Sending progress", showBackground = true)
@Preview(name = "Sending progress", showBackground = true, uiMode = UI_MODE_NIGHT_YES)
private fun PreviewSelectiveDisclosureSendingProgress() {
    WalletUIAppTheme {
        SelectiveDisclosureSheet(
            modifier = Modifier.fillMaxSize(),
            title = "Trusted verifier 'Reader' is requesting the following information",
            isSendingInProgress = true,
            sheetData = listOf(
                SelectiveDisclosureSheetData(
                    credentialName = "Credential Name",
                    documentName = "Driving Licence  |  mDL",
                    requestedItems = (1..11).map {
                        SelectiveDisclosureSheetData.DocumentItem(
                            "Property $it",
                            DocItem("$it", "namespace"),
                            isPresent = false
                        )
                    }
                )
            )
        )
    }
}