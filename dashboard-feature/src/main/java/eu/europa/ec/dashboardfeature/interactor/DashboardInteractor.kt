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

import android.bluetooth.BluetoothAdapter
import eu.europa.ec.businesslogic.config.WalletCoreConfig
import eu.europa.ec.businesslogic.controller.walletcore.WalletCoreDocumentsController
import eu.europa.ec.businesslogic.extension.safeAsync
import eu.europa.ec.businesslogic.util.toDateFormatted
import eu.europa.ec.commonfeature.model.DocumentUi
import eu.europa.ec.commonfeature.model.toDocumentTypeUi
import eu.europa.ec.commonfeature.ui.document_details.model.DocumentJsonKeys
import eu.europa.ec.commonfeature.util.extractValueFromDocumentOrEmpty
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

sealed class DashboardInteractorPartialState {
    data class Success(
        val documents: List<DocumentUi>,
        val userFirstName: String,
        val userBase64Portrait: String,
    ) : DashboardInteractorPartialState()

    data class Failure(val error: String) : DashboardInteractorPartialState()
}

interface DashboardInteractor {
    fun getDocuments(): Flow<DashboardInteractorPartialState>
    fun isBleAvailable(): Boolean
    fun isBleCentralClientModeEnabled(): Boolean
}

class DashboardInteractorImpl(
    private val resourceProvider: ResourceProvider,
    private val walletCoreDocumentsController: WalletCoreDocumentsController,
    private val walletCoreConfig: WalletCoreConfig
) : DashboardInteractor {

    private val genericErrorMsg
        get() = resourceProvider.genericErrorMessage()

    override fun isBleAvailable(): Boolean = BluetoothAdapter.getDefaultAdapter()?.isEnabled == true

    override fun isBleCentralClientModeEnabled(): Boolean =
        walletCoreConfig.config.bleCentralClientModeEnabled

    override fun getDocuments(): Flow<DashboardInteractorPartialState> = flow {
        var userFirstName = ""
        var userImage = ""
        val documents = walletCoreDocumentsController.getAllDocuments()
        val documentsUi = documents.map { document ->

            var documentExpirationDate = extractValueFromDocumentOrEmpty(
                document = document,
                key = DocumentJsonKeys.EXPIRY_DATE
            )
            documentExpirationDate = if (documentExpirationDate.isNotBlank()) {
                documentExpirationDate.toDateFormatted().toString()
            } else {
                resourceProvider.getString(R.string.dashboard_document_no_expiration_found)
            }

            if (userFirstName.isBlank()) {
                userFirstName = extractValueFromDocumentOrEmpty(
                    document = document,
                    key = DocumentJsonKeys.FIRST_NAME
                )
            }

            if (userImage.isBlank()) {
                userImage = extractValueFromDocumentOrEmpty(
                    document = document,
                    key = DocumentJsonKeys.PORTRAIT
                )
            }

            return@map DocumentUi(
                documentId = document.id,
                documentName = document.name,
                documentType = document.docType.toDocumentTypeUi(),
                documentImage = "",
                documentExpirationDateFormatted = documentExpirationDate,
                documentDetails = emptyList()
            )
        }
        emit(
            DashboardInteractorPartialState.Success(
                documents = documentsUi,
                userFirstName = userFirstName,
                userBase64Portrait = userImage
            )
        )
    }.safeAsync {
        DashboardInteractorPartialState.Failure(
            error = it.localizedMessage ?: genericErrorMsg
        )
    }
}