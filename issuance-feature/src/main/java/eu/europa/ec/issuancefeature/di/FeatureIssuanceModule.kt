/*
 * Copyright (c) 2026 European Commission
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

package eu.europa.ec.issuancefeature.di

import eu.europa.ec.businesslogic.config.ConfigLogic
import eu.europa.ec.businesslogic.provider.UuidProvider
import eu.europa.ec.commonfeature.interactor.DeviceAuthenticationInteractor
import eu.europa.ec.corelogic.controller.WalletCoreDocumentsController
import eu.europa.ec.issuancefeature.interactor.AddDocumentInteractor
import eu.europa.ec.issuancefeature.interactor.AddDocumentInteractorImpl
import eu.europa.ec.issuancefeature.interactor.DocumentIssuanceSuccessInteractor
import eu.europa.ec.issuancefeature.interactor.DocumentIssuanceSuccessInteractorImpl
import eu.europa.ec.issuancefeature.interactor.DocumentOfferInteractor
import eu.europa.ec.issuancefeature.interactor.DocumentOfferInteractorImpl
import eu.europa.ec.issuancefeature.ui.add.AddDocumentViewModel
import eu.europa.ec.issuancefeature.ui.code.DocumentOfferCodeViewModel
import eu.europa.ec.issuancefeature.ui.offer.DocumentOfferViewModel
import eu.europa.ec.issuancefeature.ui.success.DocumentIssuanceSuccessViewModel
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.config.ConfigUILogic
import eu.europa.ec.uilogic.serializer.UiSerializer
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.scopedOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.koin.mp.KoinPlatform

private const val CREDENTIAL_OFFER_ISSUANCE_SCOPE_ID = "credential_offer_scope_id"

val featureIssuanceModule = module {
    factoryOf(::provideAddDocumentInteractor)
    factoryOf(::provideDocumentIssuanceSuccessInteractor)

    scope<CredentialOfferIssuanceScope> {
        scopedOf(::provideDocumentOfferInteractor)
    }

    viewModel { (issuanceConfig: String) ->
        AddDocumentViewModel(get(), get(), get(), issuanceConfig)
    }
    viewModel { (offerCodeSerializedConfig: String) ->
        DocumentOfferCodeViewModel(get(), get(), offerCodeSerializedConfig)
    }
    viewModel { (offerSerializedConfig: String) ->
        DocumentOfferViewModel(get(), get(), offerSerializedConfig)
    }
    viewModel { (issuanceSuccessSerializedConfig: String) ->
        DocumentIssuanceSuccessViewModel(get(), get(), issuanceSuccessSerializedConfig)
    }
}

fun provideAddDocumentInteractor(
    walletCoreDocumentsController: WalletCoreDocumentsController,
    resourceProvider: ResourceProvider,
    deviceAuthenticationInteractor: DeviceAuthenticationInteractor,
    uiSerializer: UiSerializer
): AddDocumentInteractor =
    AddDocumentInteractorImpl(
        walletCoreDocumentsController,
        deviceAuthenticationInteractor,
        resourceProvider,
        uiSerializer
    )

fun provideDocumentIssuanceSuccessInteractor(
    walletCoreDocumentsController: WalletCoreDocumentsController,
    resourceProvider: ResourceProvider,
    uuIdProvider: UuidProvider,
    configUILogic: ConfigUILogic,
): DocumentIssuanceSuccessInteractor = DocumentIssuanceSuccessInteractorImpl(
    walletCoreDocumentsController,
    resourceProvider,
    uuIdProvider,
    configUILogic,
)

fun provideDocumentOfferInteractor(
    walletCoreDocumentsController: WalletCoreDocumentsController,
    resourceProvider: ResourceProvider,
    deviceAuthenticationInteractor: DeviceAuthenticationInteractor,
    uiSerializer: UiSerializer,
    configLogic: ConfigLogic
): DocumentOfferInteractor =
    DocumentOfferInteractorImpl(
        walletCoreDocumentsController,
        deviceAuthenticationInteractor,
        resourceProvider,
        uiSerializer,
        configLogic
    )

class CredentialOfferIssuanceScope

fun getOrCreateCredentialOfferScope(scopeId: String = CREDENTIAL_OFFER_ISSUANCE_SCOPE_ID): org.koin.core.scope.Scope =
    KoinPlatform.getKoin()
        .getOrCreateScope<CredentialOfferIssuanceScope>(scopeId)

fun getOrNullCredentialOfferScope(scopeId: String = CREDENTIAL_OFFER_ISSUANCE_SCOPE_ID): org.koin.core.scope.Scope? =
    KoinPlatform.getKoin().getScopeOrNull(scopeId)