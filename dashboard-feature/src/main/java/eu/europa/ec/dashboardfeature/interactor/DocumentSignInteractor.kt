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

package eu.europa.ec.dashboardfeature.interactor

import android.content.Context
import android.net.Uri
import eu.europa.ec.dashboardfeature.model.SignDocumentButtonUi
import eu.europa.ec.eudi.rqesui.infrastructure.DocumentUri
import eu.europa.ec.eudi.rqesui.infrastructure.EudiRQESUi
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.ListItemData
import eu.europa.ec.uilogic.component.ListItemMainContentData
import eu.europa.ec.uilogic.component.ListItemTrailingContentData

interface DocumentSignInteractor {
    fun launchRqesSdk(context: Context, uri: Uri)
    fun getUiItem(): SignDocumentButtonUi
}

class DocumentSignInteractorImpl(
    private val resourceProvider: ResourceProvider,
) : DocumentSignInteractor {

    override fun launchRqesSdk(context: Context, uri: Uri) {
        EudiRQESUi.initiate(
            context = context,
            documentUri = DocumentUri(uri)
        )
    }

    override fun getUiItem(): SignDocumentButtonUi {
        return SignDocumentButtonUi(
            data = ListItemData(
                itemId = resourceProvider.getString(R.string.document_sign_select_document_button_id),
                mainContentData = ListItemMainContentData.Text(
                    text = resourceProvider.getString(R.string.document_sign_select_document)
                ),
                trailingContentData = ListItemTrailingContentData.Icon(
                    iconData = AppIcons.Add
                ),
            )
        )
    }
}