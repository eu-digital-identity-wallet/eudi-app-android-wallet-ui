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

package eu.europa.ec.authenticationlogic.di

import android.content.Context
import android.provider.Settings
import eu.europa.ec.authenticationlogic.config.AuthenticationConfig
import eu.europa.ec.authenticationlogic.config.AuthenticationConfigImpl
import eu.europa.ec.authenticationlogic.config.StorageConfig
import eu.europa.ec.authenticationlogic.config.StorageConfigImpl
import eu.europa.ec.authenticationlogic.controller.authentication.BiometricAuthenticationController
import eu.europa.ec.authenticationlogic.controller.authentication.BiometricAuthenticationControllerImpl
import eu.europa.ec.authenticationlogic.controller.authentication.DeviceAuthenticationController
import eu.europa.ec.authenticationlogic.controller.authentication.DeviceAuthenticationControllerImpl
import eu.europa.ec.authenticationlogic.controller.device.DeviceLockController
import eu.europa.ec.authenticationlogic.controller.device.DeviceLockControllerImpl
import eu.europa.ec.authenticationlogic.controller.session.SessionController
import eu.europa.ec.authenticationlogic.controller.session.SessionControllerImpl
import eu.europa.ec.authenticationlogic.controller.storage.BiometryStorageController
import eu.europa.ec.authenticationlogic.controller.storage.BiometryStorageControllerImpl
import eu.europa.ec.authenticationlogic.controller.storage.PinStorageController
import eu.europa.ec.authenticationlogic.controller.storage.PinStorageControllerImpl
import eu.europa.ec.authenticationlogic.controller.throttle.PinThrottleController
import eu.europa.ec.authenticationlogic.controller.throttle.PinThrottleControllerImpl
import eu.europa.ec.authenticationlogic.storage.PrefsBiometryStorageProvider
import eu.europa.ec.authenticationlogic.storage.PrefsPinStorageProvider
import eu.europa.ec.authenticationlogic.storage.PrefsPinThrottleProvider
import eu.europa.ec.businesslogic.controller.crypto.CryptoController
import eu.europa.ec.businesslogic.controller.storage.PrefsController
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val logicAuthenticationModule = module {
    singleOf(::provideAuthenticationConfig)
    factoryOf(::provideDeviceLockController)
    singleOf(::provideSessionController)
    singleOf(::provideStorageConfig)
    factoryOf(::provideBiometricAuthenticationController)
    factoryOf(::provideDeviceAuthenticationController)
    factoryOf(::providePinStorageController)
    factoryOf(::provideBiometryStorageController)
    factoryOf(::providePinThrottleController)
}

fun provideAuthenticationConfig(): AuthenticationConfig = AuthenticationConfigImpl()

fun provideDeviceLockController(context: Context): DeviceLockController =
    DeviceLockControllerImpl(context)

fun provideSessionController(
    authenticationConfig: AuthenticationConfig
): SessionController = SessionControllerImpl(authenticationConfig)

fun provideStorageConfig(
    context: Context,
    prefsController: PrefsController,
    authenticationConfig: AuthenticationConfig,
): StorageConfig = StorageConfigImpl(
    pinImpl = PrefsPinStorageProvider(prefsController),
    biometryImpl = PrefsBiometryStorageProvider(prefsController),
    pinThrottleImpl = PrefsPinThrottleProvider(
        prefsController = prefsController,
        authenticationConfig = authenticationConfig,
        bootCount = {
            Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, -1)
        },
    )
)

fun provideBiometricAuthenticationController(
    cryptoController: CryptoController,
    biometryStorageController: BiometryStorageController,
    resourceProvider: ResourceProvider
): BiometricAuthenticationController =
    BiometricAuthenticationControllerImpl(
        resourceProvider,
        cryptoController,
        biometryStorageController
    )

fun provideDeviceAuthenticationController(
    resourceProvider: ResourceProvider,
    biometricAuthenticationController: BiometricAuthenticationController
): DeviceAuthenticationController =
    DeviceAuthenticationControllerImpl(
        resourceProvider,
        biometricAuthenticationController
    )

fun providePinStorageController(
    storageConfig: StorageConfig
): PinStorageController = PinStorageControllerImpl(storageConfig)

fun provideBiometryStorageController(
    storageConfig: StorageConfig
): BiometryStorageController = BiometryStorageControllerImpl(storageConfig)

fun providePinThrottleController(
    storageConfig: StorageConfig
): PinThrottleController = PinThrottleControllerImpl(storageConfig)