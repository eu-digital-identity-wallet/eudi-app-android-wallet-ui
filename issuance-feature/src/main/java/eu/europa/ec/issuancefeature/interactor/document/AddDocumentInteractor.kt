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

package eu.europa.ec.issuancefeature.interactor.document

import android.content.Context
import eu.europa.ec.businesslogic.controller.biometry.UserAuthenticationCorePayload
import eu.europa.ec.businesslogic.controller.biometry.BiometricsAvailability
import eu.europa.ec.businesslogic.controller.biometry.UserAuthenticationBiometricResult
import eu.europa.ec.businesslogic.controller.walletcore.AddSampleDataPartialState
import eu.europa.ec.businesslogic.controller.walletcore.IssuanceMethod
import eu.europa.ec.businesslogic.controller.walletcore.IssueDocumentPartialState
import eu.europa.ec.businesslogic.controller.walletcore.WalletCoreDocumentsController
import eu.europa.ec.businesslogic.extension.safeAsync
import eu.europa.ec.commonfeature.config.IssuanceFlowUiConfig
import eu.europa.ec.commonfeature.interactor.UserAuthenticationInteractor
import eu.europa.ec.commonfeature.model.DocumentOptionItemUi
import eu.europa.ec.commonfeature.model.DocumentTypeUi
import eu.europa.ec.commonfeature.model.toDocumentTypeUi
import eu.europa.ec.commonfeature.model.toUiName
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.component.AppIcons
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

sealed class AddDocumentInteractorPartialState {
    data class Success(val options: List<DocumentOptionItemUi>) :
        AddDocumentInteractorPartialState()

    data class Failure(val error: String) : AddDocumentInteractorPartialState()
}

interface AddDocumentInteractor {
    fun getAddDocumentOption(flowType: IssuanceFlowUiConfig): Flow<AddDocumentInteractorPartialState>

    fun issueDocument(
        issuanceMethod: IssuanceMethod,
        documentType: String
    ): Flow<IssueDocumentPartialState>

    fun addSampleData(): Flow<AddSampleDataPartialState>

    fun handleUserAuth(
        context: Context, payload: UserAuthenticationCorePayload,
        userAuthenticationBiometricResult: UserAuthenticationBiometricResult
    )
}

class AddDocumentInteractorImpl(
    private val walletCoreDocumentsController: WalletCoreDocumentsController,
    private val userAuthenticationInteractor: UserAuthenticationInteractor,
    private val resourceProvider: ResourceProvider,
) : AddDocumentInteractor {
    private val genericErrorMsg
        get() = resourceProvider.genericErrorMessage()

    override fun getAddDocumentOption(flowType: IssuanceFlowUiConfig): Flow<AddDocumentInteractorPartialState> =
        flow {
            val options = mutableListOf(
                DocumentOptionItemUi(
                    text = DocumentTypeUi.PID.toUiName(resourceProvider),
                    icon = AppIcons.Id,
                    type = DocumentTypeUi.PID,
                    available = !hasDocument(DocumentTypeUi.PID)
                ),
                DocumentOptionItemUi(
                    text = DocumentTypeUi.MDL.toUiName(resourceProvider),
                    icon = AppIcons.Id,
                    type = DocumentTypeUi.MDL,
                    available = canCreateMdl(flowType)
                )
            )
            if (flowType == IssuanceFlowUiConfig.NO_DOCUMENT) {
                options.add(
                    DocumentOptionItemUi(
                        text = DocumentTypeUi.SAMPLE_DOCUMENTS.toUiName(resourceProvider),
                        icon = AppIcons.Id,
                        type = DocumentTypeUi.SAMPLE_DOCUMENTS,
                        available = true
                    )
                )
            }

            emit(
                AddDocumentInteractorPartialState.Success(
                    options = options
                )
            )
        }.safeAsync {
            AddDocumentInteractorPartialState.Failure(
                error = it.localizedMessage ?: genericErrorMsg
            )
        }

    override fun issueDocument(
        issuanceMethod: IssuanceMethod,
        documentType: String
    ): Flow<IssueDocumentPartialState> =
        walletCoreDocumentsController.issueDocument(
            issuanceMethod = issuanceMethod,
            documentType = documentType
        )

    override fun addSampleData(): Flow<AddSampleDataPartialState> =
        walletCoreDocumentsController.addSampleData()

    override fun handleUserAuth(
        context: Context, payload: UserAuthenticationCorePayload,
        userAuthenticationBiometricResult: UserAuthenticationBiometricResult
    ) {
        userAuthenticationInteractor.getBiometricsAvailability {
            when (it) {
                is BiometricsAvailability.CanAuthenticate -> {
                    userAuthenticationInteractor.authenticateWithBiometrics(
                        context,
                        payload,
                        userAuthenticationBiometricResult
                    )
                }

                is BiometricsAvailability.NonEnrolled -> {
                    userAuthenticationInteractor.authenticateWithBiometrics(
                        context,
                        payload,
                        userAuthenticationBiometricResult
                    )
                }

                is BiometricsAvailability.Failure -> {
                    payload.onFailure()
                }
            }
        }
    }

    private fun hasDocument(documentTypeUi: DocumentTypeUi): Boolean {
        val documents = walletCoreDocumentsController.getAllDocuments()
        return if (documents.isNotEmpty()) {
            documents.any { it.docType.toDocumentTypeUi() == documentTypeUi }
        } else {
            false
        }
    }

    private fun canCreateMdl(flowType: IssuanceFlowUiConfig): Boolean {
        return if (flowType == IssuanceFlowUiConfig.NO_DOCUMENT) {
            false
        } else {
            !hasDocument(DocumentTypeUi.MDL)
        }
    }
}