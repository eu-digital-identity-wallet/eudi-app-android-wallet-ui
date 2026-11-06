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

package eu.europa.ec.proximityfeature.di

import eu.europa.ec.businesslogic.provider.UuidProvider
import eu.europa.ec.commonfeature.interactor.DeviceAuthenticationInteractor
import eu.europa.ec.corelogic.controller.WalletCoreDocumentsController
import eu.europa.ec.proximityfeature.interactor.ProximityLoadingInteractor
import eu.europa.ec.proximityfeature.interactor.ProximityLoadingInteractorImpl
import eu.europa.ec.proximityfeature.interactor.ProximityQRInteractor
import eu.europa.ec.proximityfeature.interactor.ProximityQRInteractorImpl
import eu.europa.ec.proximityfeature.interactor.ProximityRequestInteractor
import eu.europa.ec.proximityfeature.interactor.ProximityRequestInteractorImpl
import eu.europa.ec.proximityfeature.interactor.ProximitySuccessInteractor
import eu.europa.ec.proximityfeature.interactor.ProximitySuccessInteractorImpl
import eu.europa.ec.proximityfeature.ui.loading.ProximityLoadingViewModel
import eu.europa.ec.proximityfeature.ui.qr.ProximityQRViewModel
import eu.europa.ec.proximityfeature.ui.request.ProximityRequestViewModel
import eu.europa.ec.proximityfeature.ui.success.ProximitySuccessViewModel
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.config.ConfigUILogic
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val featureProximityModule = module {
    factoryOf(::provideProximityQRInteractor)
    factoryOf(::provideProximityRequestInteractor)
    factoryOf(::provideProximityLoadingInteractor)
    factoryOf(::provideProximitySuccessInteractor)

    viewModel { (presentationScopeId: String) ->
        ProximityLoadingViewModel(get(), get(), presentationScopeId)
    }
    viewModel { (requestUriConfigRaw: String) ->
        ProximityQRViewModel(get(), get(), requestUriConfigRaw)
    }
    viewModel { (presentationScopeId: String) ->
        ProximityRequestViewModel(get(), get(), get(), presentationScopeId)
    }
    viewModel { (presentationScopeId: String) ->
        ProximitySuccessViewModel(get(), presentationScopeId)
    }
}

fun provideProximityQRInteractor(
    resourceProvider: ResourceProvider
): ProximityQRInteractor =
    ProximityQRInteractorImpl(resourceProvider)

fun provideProximityRequestInteractor(
    resourceProvider: ResourceProvider,
    uuidProvider: UuidProvider,
    walletCoreDocumentsController: WalletCoreDocumentsController,
    configUILogic: ConfigUILogic,
): ProximityRequestInteractor =
    ProximityRequestInteractorImpl(
        resourceProvider,
        uuidProvider,
        walletCoreDocumentsController,
        configUILogic,
    )

fun provideProximityLoadingInteractor(
    deviceAuthenticationInteractor: DeviceAuthenticationInteractor
): ProximityLoadingInteractor =
    ProximityLoadingInteractorImpl(deviceAuthenticationInteractor)

fun provideProximitySuccessInteractor(
    walletCoreDocumentsController: WalletCoreDocumentsController,
    resourceProvider: ResourceProvider,
    uuidProvider: UuidProvider,
    configUILogic: ConfigUILogic,
): ProximitySuccessInteractor {
    return ProximitySuccessInteractorImpl(
        walletCoreDocumentsController,
        resourceProvider,
        uuidProvider,
        configUILogic,
    )
}