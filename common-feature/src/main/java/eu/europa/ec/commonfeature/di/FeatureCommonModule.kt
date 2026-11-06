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

package eu.europa.ec.commonfeature.di

import eu.europa.ec.authenticationlogic.config.AuthenticationConfig
import eu.europa.ec.authenticationlogic.controller.authentication.BiometricAuthenticationController
import eu.europa.ec.authenticationlogic.controller.authentication.DeviceAuthenticationController
import eu.europa.ec.authenticationlogic.controller.device.DeviceLockController
import eu.europa.ec.authenticationlogic.controller.session.SessionController
import eu.europa.ec.authenticationlogic.controller.storage.BiometryStorageController
import eu.europa.ec.authenticationlogic.controller.storage.PinStorageController
import eu.europa.ec.authenticationlogic.controller.throttle.PinThrottleController
import eu.europa.ec.businesslogic.controller.storage.PrefKeys
import eu.europa.ec.businesslogic.validator.FormValidator
import eu.europa.ec.commonfeature.interactor.BiometricInteractor
import eu.europa.ec.commonfeature.interactor.BiometricInteractorImpl
import eu.europa.ec.commonfeature.interactor.DeviceAuthenticationInteractor
import eu.europa.ec.commonfeature.interactor.DeviceAuthenticationInteractorImpl
import eu.europa.ec.commonfeature.interactor.QrScanInteractor
import eu.europa.ec.commonfeature.interactor.QrScanInteractorImpl
import eu.europa.ec.commonfeature.interactor.QuickPinInteractor
import eu.europa.ec.commonfeature.interactor.QuickPinInteractorImpl
import eu.europa.ec.commonfeature.interactor.TrustMarkInteractor
import eu.europa.ec.commonfeature.interactor.TrustMarkInteractorImpl
import eu.europa.ec.commonfeature.model.PinFlow
import eu.europa.ec.commonfeature.ui.biometric.BiometricViewModel
import eu.europa.ec.commonfeature.ui.pin.PinViewModel
import eu.europa.ec.commonfeature.ui.qr_scan.QrScanViewModel
import eu.europa.ec.commonfeature.ui.success.SuccessViewModel
import eu.europa.ec.commonfeature.ui.trustmark.TrustMarkViewModel
import eu.europa.ec.corelogic.controller.WalletCoreTrustMarkController
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val featureCommonModule = module {
    factoryOf(::provideTrustMarkInteractor)
    factoryOf(::provideQuickPinInteractor)
    factoryOf(::provideBiometricInteractor)
    factoryOf(::provideDeviceAuthenticationInteractor)
    factoryOf(::provideQrScanInteractor)

    viewModel { (biometricConfig: String) ->
        BiometricViewModel(get(), get(), get(), biometricConfig)
    }
    viewModel { (pinFlow: PinFlow) -> PinViewModel(get(), get(), get(), pinFlow) }
    viewModel { (qrScannedConfig: String) ->
        QrScanViewModel(get(), get(), get(), qrScannedConfig)
    }
    viewModel { (successConfig: String) -> SuccessViewModel(get(), successConfig) }
    viewModel { (trustMarkConfig: String) ->
        TrustMarkViewModel(get(), get(), trustMarkConfig)
    }
}

fun provideTrustMarkInteractor(
    walletCoreTrustMarkController: WalletCoreTrustMarkController,
    prefKeys: PrefKeys,
    resourceProvider: ResourceProvider,
): TrustMarkInteractor = TrustMarkInteractorImpl(
    walletCoreTrustMarkController = walletCoreTrustMarkController,
    prefKeys = prefKeys,
    resourceProvider = resourceProvider,
)

fun provideQuickPinInteractor(
    pinStorageController: PinStorageController,
    resourceProvider: ResourceProvider,
    pinThrottleController: PinThrottleController,
    authenticationConfig: AuthenticationConfig,
    sessionController: SessionController,
    deviceLockController: DeviceLockController,
): QuickPinInteractor {
    return QuickPinInteractorImpl(
        pinStorageController,
        resourceProvider,
        pinThrottleController,
        authenticationConfig,
        sessionController,
        deviceLockController,
    )
}

fun provideBiometricInteractor(
    biometryStorageController: BiometryStorageController,
    biometricAuthenticationController: BiometricAuthenticationController,
    quickPinInteractor: QuickPinInteractor,
    pinThrottleController: PinThrottleController,
    authenticationConfig: AuthenticationConfig,
    sessionController: SessionController
): BiometricInteractor {
    return BiometricInteractorImpl(
        biometryStorageController,
        biometricAuthenticationController,
        quickPinInteractor,
        pinThrottleController,
        authenticationConfig,
        sessionController
    )
}

fun provideDeviceAuthenticationInteractor(
    deviceAuthenticationController: DeviceAuthenticationController
): DeviceAuthenticationInteractor {
    return DeviceAuthenticationInteractorImpl(deviceAuthenticationController)
}

fun provideQrScanInteractor(
    formValidator: FormValidator
): QrScanInteractor {
    return QrScanInteractorImpl(formValidator)
}