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

package eu.europa.ec.issuancefeature.ui.document.add.transformer

import eu.europa.ec.commonfeature.model.DocumentOptionItemUi
import eu.europa.ec.commonfeature.model.DocumentOptionListItemHolder
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.ListItemData
import eu.europa.ec.uilogic.component.ListItemTrailingContentData
import eu.europa.ec.uilogic.component.MainContentData

internal object AddDocumentTransformer {
    fun List<DocumentOptionItemUi>.transformToAddDocumentUi(): List<DocumentOptionListItemHolder> {
        return this.map { item ->
            DocumentOptionListItemHolder(
                listItemData = ListItemData(
                    itemId = item.configId,
                    mainContentData = MainContentData.Text(text = item.text),
                    trailingContentData = ListItemTrailingContentData.Icon(iconData = AppIcons.Add)
                ),
                available = item.available
            )
        }
    }
}