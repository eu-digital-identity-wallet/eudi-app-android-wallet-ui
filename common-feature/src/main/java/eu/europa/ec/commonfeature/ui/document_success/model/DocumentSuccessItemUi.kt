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

package eu.europa.ec.commonfeature.ui.document_success.model

import eu.europa.ec.uilogic.component.ListItemData
import eu.europa.ec.uilogic.component.ListItemMainContentData
import eu.europa.ec.uilogic.component.wrap.ExpandableListItem

data class DocumentSuccessItemUi(
    val headerUi: ExpandableListItem.NestedListItemData = ExpandableListItem.NestedListItemData(
        //TODO remove default value later
        header = ListItemData(
            itemId = "tempId",
            mainContentData = ListItemMainContentData.Text(text = "tempText"),
        ),
        nestedItems = emptyList(),
        isExpanded = false,
    ),
)