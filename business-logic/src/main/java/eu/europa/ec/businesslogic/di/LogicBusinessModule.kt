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

package eu.europa.ec.businesslogic.di

import android.content.Context
import eu.europa.ec.businesslogic.config.ConfigLogic
import eu.europa.ec.businesslogic.config.ConfigLogicImpl
import eu.europa.ec.businesslogic.config.ConfigSecurityLogic
import eu.europa.ec.businesslogic.config.ConfigSecurityLogicImpl
import eu.europa.ec.businesslogic.config.WalletCoreConfig
import eu.europa.ec.businesslogic.config.WalletCoreConfigImpl
import eu.europa.ec.businesslogic.controller.biometry.BiometricController
import eu.europa.ec.businesslogic.controller.biometry.BiometricControllerImpl
import eu.europa.ec.businesslogic.controller.biometry.UserAuthenticationController
import eu.europa.ec.businesslogic.controller.biometry.UserAuthenticationControllerImpl
import eu.europa.ec.businesslogic.controller.crypto.CryptoController
import eu.europa.ec.businesslogic.controller.crypto.CryptoControllerImpl
import eu.europa.ec.businesslogic.controller.crypto.KeystoreController
import eu.europa.ec.businesslogic.controller.crypto.KeystoreControllerImpl
import eu.europa.ec.businesslogic.controller.log.LogController
import eu.europa.ec.businesslogic.controller.log.LogControllerImpl
import eu.europa.ec.businesslogic.controller.security.AndroidPackageController
import eu.europa.ec.businesslogic.controller.security.AndroidPackageControllerImpl
import eu.europa.ec.businesslogic.controller.security.AntiHookController
import eu.europa.ec.businesslogic.controller.security.AntiHookControllerImpl
import eu.europa.ec.businesslogic.controller.security.RootController
import eu.europa.ec.businesslogic.controller.security.RootControllerImpl
import eu.europa.ec.businesslogic.controller.security.SecurityController
import eu.europa.ec.businesslogic.controller.security.SecurityControllerImpl
import eu.europa.ec.businesslogic.controller.storage.PrefKeys
import eu.europa.ec.businesslogic.controller.storage.PrefKeysImpl
import eu.europa.ec.businesslogic.controller.storage.PrefsController
import eu.europa.ec.businesslogic.controller.storage.PrefsControllerImpl
import eu.europa.ec.businesslogic.controller.walletcore.WalletCoreDocumentsController
import eu.europa.ec.businesslogic.controller.walletcore.WalletCoreDocumentsControllerImpl
import eu.europa.ec.businesslogic.validator.FormValidator
import eu.europa.ec.businesslogic.validator.FormValidatorImpl
import eu.europa.ec.eudi.wallet.EudiWallet
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module
import org.koin.core.annotation.Scope
import org.koin.core.annotation.Single
import org.koin.mp.KoinPlatform.getKoin

const val PRESENTATION_SCOPE_ID = "presentation_scope_id"

@Module
@ComponentScan("eu.europa.ec.businesslogic")
class LogicBusinessModule

@Single
fun provideConfigLogic(): ConfigLogic = ConfigLogicImpl()

@Single
fun provideConfigSecurityLogic(configLogic: ConfigLogic): ConfigSecurityLogic =
    ConfigSecurityLogicImpl(configLogic)

@Single
fun provideLogController(configLogic: ConfigLogic): LogController =
    LogControllerImpl(configLogic)

@Single
fun providePrefsController(resourceProvider: ResourceProvider): PrefsController =
    PrefsControllerImpl(resourceProvider)

@Single
fun providePrefKeys(prefsController: PrefsController): PrefKeys =
    PrefKeysImpl(prefsController)

@Single
fun provideKeystoreController(
    prefKeys: PrefKeys,
    logController: LogController
): KeystoreController =
    KeystoreControllerImpl(prefKeys, logController)

@Factory
fun provideCryptoController(keystoreController: KeystoreController): CryptoController =
    CryptoControllerImpl(keystoreController)

@Factory
fun provideBiometricController(
    cryptoController: CryptoController,
    prefKeys: PrefKeys,
    resourceProvider: ResourceProvider
): BiometricController =
    BiometricControllerImpl(resourceProvider, cryptoController, prefKeys)

@Factory
fun provideUserAuthenticationController(
    resourceProvider: ResourceProvider,
    biometricController: BiometricController
): UserAuthenticationController =
    UserAuthenticationControllerImpl(resourceProvider, biometricController)

@Factory
fun provideFormValidator(logController: LogController): FormValidator =
    FormValidatorImpl(logController)

@Factory
fun provideAndroidPackageController(
    resourceProvider: ResourceProvider,
    logController: LogController
): AndroidPackageController =
    AndroidPackageControllerImpl(resourceProvider, logController)

@Factory
fun provideAntiHookController(logController: LogController): AntiHookController =
    AntiHookControllerImpl(logController)

@Factory
fun provideRootController(resourceProvider: ResourceProvider): RootController =
    RootControllerImpl(resourceProvider)

@Factory
fun provideSecurityController(
    configLogic: ConfigLogic,
    configSecurityLogic: ConfigSecurityLogic,
    rootController: RootController,
    antiHookController: AntiHookController,
    androidPackageController: AndroidPackageController
): SecurityController =
    SecurityControllerImpl(
        configLogic,
        configSecurityLogic,
        rootController,
        antiHookController,
        androidPackageController
    )

@Single
fun provideEudiWalletCore(): EudiWallet = EudiWallet

@Factory
fun provideWalletCoreDocumentsController(
    resourceProvider: ResourceProvider,
    eudiWallet: EudiWallet,
): WalletCoreDocumentsController =
    WalletCoreDocumentsControllerImpl(
        resourceProvider,
        eudiWallet
    )

@Factory
fun provideConfigWalletCore(
    context: Context
): WalletCoreConfig =
    WalletCoreConfigImpl(context)

/**
 * Koin scope that lives for all the document presentation flow. It is manually handled from the
 * ViewModels that start and participate on the presentation process
 * */
@Scope
class WalletPresentationScope

/**
 * Get Koin scope that lives during document presentation flow
 * */
fun getOrCreatePresentationScope(): org.koin.core.scope.Scope =
    getKoin().getOrCreateScope<WalletPresentationScope>(PRESENTATION_SCOPE_ID)