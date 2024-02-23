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

package eu.europa.ec.commonfeature.di

import eu.europa.ec.businesslogic.controller.biometry.BiometricController
import eu.europa.ec.businesslogic.controller.biometry.UserAuthenticationController
import eu.europa.ec.businesslogic.controller.storage.PrefKeys
import eu.europa.ec.businesslogic.validator.FormValidator
import eu.europa.ec.commonfeature.interactor.BiometricInteractor
import eu.europa.ec.commonfeature.interactor.BiometricInteractorImpl
import eu.europa.ec.commonfeature.interactor.UserAuthenticationInteractor
import eu.europa.ec.commonfeature.interactor.UserAuthenticationInteractorImpl
import eu.europa.ec.commonfeature.interactor.QuickPinInteractor
import eu.europa.ec.commonfeature.interactor.QuickPinInteractorImpl
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module

@Module
@ComponentScan("eu.europa.ec.commonfeature")
class FeatureCommonModule

@Factory
fun provideQuickPinInteractor(
    formValidator: FormValidator,
    prefKeys: PrefKeys,
    resourceProvider: ResourceProvider
): QuickPinInteractor {
    return QuickPinInteractorImpl(formValidator, prefKeys, resourceProvider)
}

@Factory
fun provideBiometricInteractor(
    prefKeys: PrefKeys,
    biometricController: BiometricController,
    quickPinInteractor: QuickPinInteractor
): BiometricInteractor {
    return BiometricInteractorImpl(prefKeys, biometricController, quickPinInteractor)
}

@Factory
fun provideDeviceBiometricInteractor(
    userAuthenticationController: UserAuthenticationController
): UserAuthenticationInteractor {
    return UserAuthenticationInteractorImpl(userAuthenticationController)
}