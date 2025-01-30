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

package eu.europa.ec.dashboardfeature.extensions

import eu.europa.ec.commonfeature.model.DocumentUiIssuanceState
import eu.europa.ec.corelogic.model.DocumentCategory
import eu.europa.ec.corelogic.model.DocumentIdentifier
import eu.europa.ec.dashboardfeature.model.DocumentUi
import eu.europa.ec.dashboardfeature.model.FilterableAttributes
import eu.europa.ec.dashboardfeature.model.FilterableDocumentItem
import eu.europa.ec.dashboardfeature.model.FilterableDocuments
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.component.DualSelectorButton
import eu.europa.ec.uilogic.component.ListItemData
import eu.europa.ec.uilogic.component.ListItemMainContentData

internal fun FilterableDocuments.search(query: String): FilterableDocuments {
    val result = documents.filter {
        (it.itemUi.uiData.mainContentData as ListItemMainContentData.Text).text.lowercase()
            .contains(query.lowercase())
    }
    return copy(documents = result.toMutableList())
}

internal fun FilterableDocuments.getEmptyUIifEmptyList(resourceProvider: ResourceProvider): FilterableDocuments {
    return copy(documents = documents.ifEmpty {
        listOf(
            FilterableDocumentItem(
                itemUi = DocumentUi(
                    documentIssuanceState = DocumentUiIssuanceState.Issued,
                    uiData = ListItemData(
                        itemId = "",
                        mainContentData = ListItemMainContentData.Text(
                            resourceProvider.getString(
                                R.string.documents_screen_search_no_results
                            )
                        ),
                        overlineText = null,
                        supportingText = null,
                        leadingContentData = null,
                        trailingContentData = null
                    ),
                    documentIdentifier = DocumentIdentifier.OTHER(
                        formatType = ""
                    ),
                    documentCategory = DocumentCategory.Other,
                ),
                filterableAttributes = FilterableAttributes(
                    issuedDate = null,
                    expiryDate = null,
                    issuer = null
                )
            )
        )
    })
}

internal fun <T, R : Comparable<R>> List<T>.sortByOrder(
    sortOrder: DualSelectorButton,
    selector: (T) -> R?,
): List<T> {
    return when (sortOrder) {
        DualSelectorButton.FIRST -> sortedBy(selector)
        DualSelectorButton.SECOND -> sortedByDescending(selector)
    }
}