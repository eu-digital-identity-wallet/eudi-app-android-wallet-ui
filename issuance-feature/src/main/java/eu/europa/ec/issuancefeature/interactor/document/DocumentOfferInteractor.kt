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
import eu.europa.ec.authenticationlogic.controller.authentication.BiometricsAvailability
import eu.europa.ec.authenticationlogic.controller.authentication.DeviceAuthenticationResult
import eu.europa.ec.authenticationlogic.model.BiometricCrypto
import eu.europa.ec.businesslogic.extension.safeAsync
import eu.europa.ec.businesslogic.util.safeLet
import eu.europa.ec.commonfeature.config.SuccessUIConfig
import eu.europa.ec.commonfeature.interactor.DeviceAuthenticationInteractor
import eu.europa.ec.commonfeature.ui.request.model.DocumentItemUi
import eu.europa.ec.corelogic.controller.IssueDocumentsPartialState
import eu.europa.ec.corelogic.controller.ResolveDocumentOfferPartialState
import eu.europa.ec.corelogic.controller.WalletCoreDocumentsController
import eu.europa.ec.corelogic.extension.documentIdentifier
import eu.europa.ec.corelogic.extension.getIssuerName
import eu.europa.ec.corelogic.extension.getName
import eu.europa.ec.corelogic.model.DocumentIdentifier
import eu.europa.ec.eudi.openid4vci.TxCodeInputMode
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.resourceslogic.theme.values.ThemeColors
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.config.ConfigNavigation
import eu.europa.ec.uilogic.navigation.CommonScreens
import eu.europa.ec.uilogic.navigation.helper.generateComposableArguments
import eu.europa.ec.uilogic.navigation.helper.generateComposableNavigationLink
import eu.europa.ec.uilogic.serializer.UiSerializer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

sealed class ResolveDocumentOfferInteractorPartialState {
    data class Success(
        val documents: List<DocumentItemUi>,
        val issuerName: String,
        val txCodeLength: Int?
    ) : ResolveDocumentOfferInteractorPartialState()

    data class NoDocument(val issuerName: String) : ResolveDocumentOfferInteractorPartialState()
    data class Failure(val errorMessage: String) : ResolveDocumentOfferInteractorPartialState()
}

sealed class IssueDocumentsInteractorPartialState {
    data class Success(
        val successRoute: String,
    ) : IssueDocumentsInteractorPartialState()

    data class DeferredSuccess(
        val successRoute: String,
    ) : IssueDocumentsInteractorPartialState()

    data class Failure(val errorMessage: String) : IssueDocumentsInteractorPartialState()

    data class UserAuthRequired(
        val crypto: BiometricCrypto,
        val resultHandler: DeviceAuthenticationResult
    ) : IssueDocumentsInteractorPartialState()
}

interface DocumentOfferInteractor {
    fun resolveDocumentOffer(offerUri: String): Flow<ResolveDocumentOfferInteractorPartialState>

    fun issueDocuments(
        offerUri: String,
        issuerName: String,
        navigation: ConfigNavigation,
        txCode: String? = null
    ): Flow<IssueDocumentsInteractorPartialState>

    fun handleUserAuthentication(
        context: Context,
        crypto: BiometricCrypto,
        notifyOnAuthenticationFailure: Boolean,
        resultHandler: DeviceAuthenticationResult
    )

    fun resumeOpenId4VciWithAuthorization(uri: String)
}

class DocumentOfferInteractorImpl(
    private val walletCoreDocumentsController: WalletCoreDocumentsController,
    private val deviceAuthenticationInteractor: DeviceAuthenticationInteractor,
    private val resourceProvider: ResourceProvider,
    private val uiSerializer: UiSerializer
) : DocumentOfferInteractor {

    private val genericErrorMsg
        get() = resourceProvider.genericErrorMessage()

    override fun resolveDocumentOffer(offerUri: String): Flow<ResolveDocumentOfferInteractorPartialState> =
        flow {
            walletCoreDocumentsController.resolveDocumentOffer(
                offerUri = offerUri
            ).map { response ->
                when (response) {
                    is ResolveDocumentOfferPartialState.Failure -> {
                        ResolveDocumentOfferInteractorPartialState.Failure(errorMessage = response.errorMessage)
                    }

                    is ResolveDocumentOfferPartialState.Success -> {
                        val offerHasNoDocuments = response.offer.offeredDocuments.isEmpty()
                        if (offerHasNoDocuments) {
                            ResolveDocumentOfferInteractorPartialState.NoDocument(
                                issuerName = response.offer.getIssuerName(
                                    resourceProvider.getLocale()
                                )
                            )
                        } else {

                            val codeMinLength = 4
                            val codeMaxLength = 6

                            safeLet(
                                response.offer.txCodeSpec?.inputMode,
                                response.offer.txCodeSpec?.length
                            ) { inputMode, length ->

                                if ((length !in codeMinLength..codeMaxLength) || inputMode == TxCodeInputMode.TEXT) {
                                    return@map ResolveDocumentOfferInteractorPartialState.Failure(
                                        errorMessage = resourceProvider.getString(
                                            R.string.issuance_document_offer_error_invalid_txcode_format,
                                            codeMinLength,
                                            codeMaxLength
                                        )
                                    )
                                }
                            }

                            val hasMainPid =
                                walletCoreDocumentsController.getMainPidDocument() != null

                            val hasPidInOffer =
                                response.offer.offeredDocuments.any { offeredDocument ->
                                    val id = offeredDocument.documentIdentifier
                                    // TODO: Re-activate once SD-JWT PID Rule book is in place in ARF.
                                    // id == DocumentIdentifier.MdocPid || id == DocumentIdentifier.SdJwtPid
                                    id == DocumentIdentifier.MdocPid
                                }

                            if (hasMainPid || hasPidInOffer) {

                                ResolveDocumentOfferInteractorPartialState.Success(
                                    documents = response.offer.offeredDocuments.map { offeredDocument ->
                                        DocumentItemUi(
                                            title = offeredDocument.getName(
                                                resourceProvider.getLocale()
                                            ).orEmpty()
                                        )
                                    },
                                    issuerName = response.offer.getIssuerName(resourceProvider.getLocale()),
                                    txCodeLength = response.offer.txCodeSpec?.length
                                )
                            } else {
                                ResolveDocumentOfferInteractorPartialState.Failure(
                                    errorMessage = resourceProvider.getString(
                                        R.string.issuance_document_offer_error_missing_pid_text
                                    )
                                )
                            }
                        }
                    }
                }
            }.collect {
                emit(it)
            }
        }.safeAsync {
            ResolveDocumentOfferInteractorPartialState.Failure(
                errorMessage = it.localizedMessage ?: genericErrorMsg
            )
        }

    override fun issueDocuments(
        offerUri: String,
        issuerName: String,
        navigation: ConfigNavigation,
        txCode: String?
    ): Flow<IssueDocumentsInteractorPartialState> =
        flow {
            walletCoreDocumentsController.issueDocumentsByOfferUri(
                offerUri = offerUri,
                txCode = txCode
            ).map { response ->
                when (response) {
                    is IssueDocumentsPartialState.Failure -> {
                        IssueDocumentsInteractorPartialState.Failure(errorMessage = response.errorMessage)
                    }

                    is IssueDocumentsPartialState.PartialSuccess -> {

                        val nonIssuedDocsNames: String =
                            response.nonIssuedDocuments.entries.map { it.value }.joinToString(
                                separator = ", ",
                                transform = {
                                    it
                                }
                            )

                        IssueDocumentsInteractorPartialState.Success(
                            successRoute = buildGenericSuccessRoute(
                                type = IssuanceSuccessType.DEFAULT,
                                subtitle = resourceProvider.getString(
                                    R.string.issuance_document_offer_partial_success_subtitle,
                                    issuerName,
                                    nonIssuedDocsNames
                                ),
                                navigation = navigation
                            )
                        )
                    }

                    is IssueDocumentsPartialState.Success -> {
                        IssueDocumentsInteractorPartialState.Success(
                            successRoute = buildGenericSuccessRoute(
                                type = IssuanceSuccessType.DEFAULT,
                                subtitle = resourceProvider.getString(
                                    R.string.issuance_document_offer_success_subtitle,
                                    issuerName
                                ),
                                navigation = navigation
                            )
                        )
                    }

                    is IssueDocumentsPartialState.UserAuthRequired -> {
                        IssueDocumentsInteractorPartialState.UserAuthRequired(
                            crypto = response.crypto,
                            resultHandler = response.resultHandler
                        )
                    }

                    is IssueDocumentsPartialState.DeferredSuccess -> {
                        IssueDocumentsInteractorPartialState.DeferredSuccess(
                            successRoute = buildGenericSuccessRoute(
                                type = IssuanceSuccessType.DEFERRED,
                                subtitle = resourceProvider.getString(
                                    R.string.issuance_document_offer_deferred_success_subtitle,
                                    issuerName
                                ),
                                navigation = navigation
                            )
                        )
                    }
                }
            }.collect {
                emit(it)
            }
        }.safeAsync {
            IssueDocumentsInteractorPartialState.Failure(
                errorMessage = it.localizedMessage ?: genericErrorMsg
            )
        }

    override fun handleUserAuthentication(
        context: Context,
        crypto: BiometricCrypto,
        notifyOnAuthenticationFailure: Boolean,
        resultHandler: DeviceAuthenticationResult
    ) {
        deviceAuthenticationInteractor.getBiometricsAvailability {
            when (it) {
                is BiometricsAvailability.CanAuthenticate -> {
                    deviceAuthenticationInteractor.authenticateWithBiometrics(
                        context = context,
                        crypto = crypto,
                        notifyOnAuthenticationFailure = notifyOnAuthenticationFailure,
                        resultHandler = resultHandler
                    )
                }

                is BiometricsAvailability.NonEnrolled -> {
                    deviceAuthenticationInteractor.launchBiometricSystemScreen()
                }

                is BiometricsAvailability.Failure -> {
                    resultHandler.onAuthenticationFailure()
                }
            }
        }
    }

    override fun resumeOpenId4VciWithAuthorization(uri: String) {
        walletCoreDocumentsController.resumeOpenId4VciWithAuthorization(uri)
    }

    private enum class IssuanceSuccessType {
        DEFAULT, DEFERRED
    }

    private fun buildGenericSuccessRoute(
        type: IssuanceSuccessType,
        subtitle: String,
        navigation: ConfigNavigation
    ): String {
        val successScreenArguments = getSuccessScreenArguments(type, subtitle, navigation)
        return generateComposableNavigationLink(
            screen = CommonScreens.Success,
            arguments = successScreenArguments
        )
    }

    private fun getSuccessScreenArguments(
        type: IssuanceSuccessType,
        subtitle: String,
        navigation: ConfigNavigation
    ): String {
        val (headerConfig, imageConfig, buttonText) = when (type) {
            IssuanceSuccessType.DEFAULT -> Triple(
                first = SuccessUIConfig.HeaderConfig(
                    title = resourceProvider.getString(R.string.issuance_document_offer_success_title),
                    color = ThemeColors.success
                ),
                second = SuccessUIConfig.ImageConfig(
                    type = SuccessUIConfig.ImageConfig.Type.DEFAULT,
                    drawableRes = null,
                    tint = ThemeColors.success,
                    contentDescription = resourceProvider.getString(R.string.content_description_success_icon)
                ),
                third = resourceProvider.getString(R.string.issuance_document_offer_success_primary_button_text)
            )

            IssuanceSuccessType.DEFERRED -> Triple(
                first = SuccessUIConfig.HeaderConfig(
                    title = resourceProvider.getString(R.string.issuance_document_offer_deferred_success_title),
                    color = ThemeColors.warning
                ),
                second = SuccessUIConfig.ImageConfig(
                    type = SuccessUIConfig.ImageConfig.Type.DRAWABLE,
                    drawableRes = AppIcons.ClockTimer.resourceId,
                    tint = ThemeColors.warning,
                    contentDescription = resourceProvider.getString(AppIcons.ClockTimer.contentDescriptionId)
                ),
                third = resourceProvider.getString(R.string.issuance_document_offer_deferred_success_primary_button_text)
            )
        }

        return generateComposableArguments(
            mapOf(
                SuccessUIConfig.serializedKeyName to uiSerializer.toBase64(
                    SuccessUIConfig(
                        headerConfig = headerConfig,
                        content = subtitle,
                        imageConfig = imageConfig,
                        buttonConfig = listOf(
                            SuccessUIConfig.ButtonConfig(
                                text = buttonText,
                                style = SuccessUIConfig.ButtonConfig.Style.PRIMARY,
                                navigation = navigation
                            )
                        ),
                        onBackScreenToNavigate = navigation,
                    ),
                    SuccessUIConfig.Parser
                ).orEmpty()
            )
        )
    }
}