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

package eu.europa.ec.presentationfeature.di

import eu.europa.ec.businesslogic.provider.UuidProvider
import eu.europa.ec.commonfeature.interactor.DeviceAuthenticationInteractor
import eu.europa.ec.corelogic.controller.WalletCoreDocumentsController
import eu.europa.ec.presentationfeature.interactor.PresentationLoadingInteractor
import eu.europa.ec.presentationfeature.interactor.PresentationLoadingInteractorImpl
import eu.europa.ec.presentationfeature.interactor.PresentationRequestInteractor
import eu.europa.ec.presentationfeature.interactor.PresentationRequestInteractorImpl
import eu.europa.ec.presentationfeature.interactor.PresentationSuccessInteractor
import eu.europa.ec.presentationfeature.interactor.PresentationSuccessInteractorImpl
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module

@Module
@Configuration
@ComponentScan("eu.europa.ec.presentationfeature")
class FeaturePresentationModule

@Factory
fun providePresentationRequestInteractor(
    resourceProvider: ResourceProvider,
    uuidProvider: UuidProvider,
    walletCoreDocumentsController: WalletCoreDocumentsController
): PresentationRequestInteractor {
    return PresentationRequestInteractorImpl(
        resourceProvider,
        uuidProvider,
        walletCoreDocumentsController
    )
}

@Factory
fun providePresentationLoadingInteractor(
    deviceAuthenticationInteractor: DeviceAuthenticationInteractor,
): PresentationLoadingInteractor {
    return PresentationLoadingInteractorImpl(
        deviceAuthenticationInteractor
    )
}

@Factory
fun providePresentationSuccessInteractor(
    walletCoreDocumentsController: WalletCoreDocumentsController,
    resourceProvider: ResourceProvider,
    uuidProvider: UuidProvider,
): PresentationSuccessInteractor {
    return PresentationSuccessInteractorImpl(
        walletCoreDocumentsController,
        resourceProvider,
        uuidProvider,
    )
}