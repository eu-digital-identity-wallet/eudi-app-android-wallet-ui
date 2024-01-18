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

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import eu.europa.ec.eudi.iso18013.transfer.DocItem

@Stable
@Immutable
data class SelectiveDisclosureSheetData(
    val credentialName: String,
    val documentName: String,
    val requestedItems: List<DocumentItem>
) {

    data class DocumentItem(
        val displayName: String,
        val requestedItem: DocItem,
        val isPresent: Boolean
    )
}