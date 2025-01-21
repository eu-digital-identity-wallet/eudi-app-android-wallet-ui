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

package eu.europa.ec.dashboardfeature.model

import eu.europa.ec.uilogic.component.DualSelectorButton
import java.time.Instant

data class FilterableAttributes(
    val issuedDate: Instant?,
    val expiryDate: Instant?,
    val issuer: String? = null,
)

data class FilterableDocumentItem(
    val itemUi: DocumentDetailsItemUi,
    val filterableAttributes: FilterableAttributes,
)

data class FilterableDocuments(
    val documents: List<FilterableDocumentItem>,
    val sortingOrder: DualSelectorButton = DualSelectorButton.FIRST,
)