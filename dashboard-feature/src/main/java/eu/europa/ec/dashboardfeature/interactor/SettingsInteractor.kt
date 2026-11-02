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

package eu.europa.ec.dashboardfeature.interactor

import android.net.Uri
import eu.europa.ec.authenticationlogic.controller.authentication.BiometricsAvailability
import eu.europa.ec.businesslogic.config.ConfigLogic
import eu.europa.ec.businesslogic.controller.log.LogController
import eu.europa.ec.businesslogic.controller.storage.PrefKeys
import eu.europa.ec.commonfeature.interactor.BiometricInteractor
import eu.europa.ec.corelogic.controller.WalletCoreDCAPIRegistrationController
import eu.europa.ec.corelogic.provider.DCAPISettingProvider
import eu.europa.ec.dashboardfeature.ui.settings.model.SettingsItemUi
import eu.europa.ec.dashboardfeature.ui.settings.model.SettingsMenuItemType
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.ListItemDataUi
import eu.europa.ec.uilogic.component.ListItemLeadingContentDataUi
import eu.europa.ec.uilogic.component.ListItemMainContentDataUi
import eu.europa.ec.uilogic.component.ListItemTrailingContentDataUi
import eu.europa.ec.uilogic.component.wrap.SwitchDataUi

interface SettingsInteractor : BiometricInteractor {
    fun getAppVersion(): String
    fun getChangelogUrl(): String?
    fun retrieveLogFileUris(): ArrayList<Uri>
    suspend fun getSettingsItemsUi(changelogUrl: String?): List<SettingsItemUi>
    suspend fun toggleBiometricsAuthentication()
    suspend fun toggleShowBatchIssuanceCounter()
    suspend fun toggleRegistrationCheck()

    /**
     * Turns the Digital Credential API on or off and applies the change at once: the wallet's
     * documents are registered with the system again, or removed from its registry.
     */
    suspend fun toggleDCAPI()
}

class SettingsInteractorImpl(
    private val biometricInteractor: BiometricInteractor,
    private val configLogic: ConfigLogic,
    private val logController: LogController,
    private val resourceProvider: ResourceProvider,
    private val prefKeys: PrefKeys,
    private val dcapiSettingProvider: DCAPISettingProvider,
    private val walletCoreDCAPIRegistrationController: WalletCoreDCAPIRegistrationController,
) : SettingsInteractor,
    BiometricInteractor by biometricInteractor {

    override fun getAppVersion(): String = configLogic.appVersion

    override fun getChangelogUrl(): String? = configLogic.changelogUrl

    override fun retrieveLogFileUris(): ArrayList<Uri> {
        return ArrayList(logController.retrieveLogFileUris())
    }

    override suspend fun getSettingsItemsUi(changelogUrl: String?): List<SettingsItemUi> {
        val deviceSupportsBiometrics = deviceSupportsBiometrics()
        val registrationCheckEnabled = getRegistrationCheckEnabled()

        return buildList {
            if (deviceSupportsBiometrics) {
                add(
                    SettingsItemUi(
                        type = SettingsMenuItemType.BIOMETRICS_AUTHENTICATION,
                        data = ListItemDataUi(
                            itemId = SettingsMenuItemType.BIOMETRICS_AUTHENTICATION.itemId,
                            mainContentData = ListItemMainContentDataUi.Text(
                                text = resourceProvider.getString(
                                    R.string.settings_screen_option_biometrics_authentication
                                )
                            ),
                            leadingContentData = ListItemLeadingContentDataUi.Icon(
                                iconData = AppIcons.TouchId
                            ),
                            trailingContentData = ListItemTrailingContentDataUi.Switch(
                                switchData = SwitchDataUi(
                                    isChecked = getBiometricUsageDecision(),
                                    enabled = true,
                                )
                            )
                        )
                    )
                )
            }

            add(
                SettingsItemUi(
                    type = SettingsMenuItemType.SHOW_BATCH_ISSUANCE_COUNTER,
                    data = ListItemDataUi(
                        itemId = SettingsMenuItemType.SHOW_BATCH_ISSUANCE_COUNTER.itemId,
                        mainContentData = ListItemMainContentDataUi.Text(
                            text = resourceProvider.getString(
                                R.string.settings_screen_option_show_batch_issuance_counter
                            )
                        ),
                        leadingContentData = ListItemLeadingContentDataUi.Icon(
                            iconData = AppIcons.BatchIssuanceCounter
                        ),
                        trailingContentData = ListItemTrailingContentDataUi.Switch(
                            switchData = SwitchDataUi(
                                isChecked = getShowBatchIssuanceCounter(),
                                enabled = true,
                            )
                        )
                    )
                )
            )

            add(
                SettingsItemUi(
                    type = SettingsMenuItemType.REGISTRATION_CHECK,
                    data = ListItemDataUi(
                        itemId = SettingsMenuItemType.REGISTRATION_CHECK.itemId,
                        mainContentData = ListItemMainContentDataUi.Text(
                            text = resourceProvider.getString(
                                R.string.settings_screen_option_registration_check
                            )
                        ),
                        leadingContentData = ListItemLeadingContentDataUi.Icon(
                            iconData = AppIcons.Verified
                        ),
                        trailingContentData = ListItemTrailingContentDataUi.Switch(
                            switchData = SwitchDataUi(
                                isChecked = registrationCheckEnabled,
                                enabled = true,
                            )
                        )
                    )
                )
            )

            if (dcapiSettingProvider.isAvailable) {
                add(
                    SettingsItemUi(
                        type = SettingsMenuItemType.DC_API,
                        data = ListItemDataUi(
                            itemId = SettingsMenuItemType.DC_API.itemId,
                            mainContentData = ListItemMainContentDataUi.Text(
                                text = resourceProvider.getString(
                                    R.string.settings_screen_option_dc_api
                                )
                            ),
                            leadingContentData = ListItemLeadingContentDataUi.Icon(
                                iconData = AppIcons.Id
                            ),
                            trailingContentData = ListItemTrailingContentDataUi.Switch(
                                switchData = SwitchDataUi(
                                    isChecked = dcapiSettingProvider.isEnabled(),
                                    enabled = true,
                                )
                            )
                        )
                    )
                )
            }

            if (logController.shouldWriteLogFile) {
                add(
                    SettingsItemUi(
                        type = SettingsMenuItemType.RETRIEVE_LOGS,
                        data = ListItemDataUi(
                            itemId = SettingsMenuItemType.RETRIEVE_LOGS.itemId,
                            mainContentData = ListItemMainContentDataUi.Text(
                                text = resourceProvider.getString(
                                    R.string.settings_screen_option_retrieve_logs
                                )
                            ),
                            leadingContentData = ListItemLeadingContentDataUi.Icon(
                                iconData = AppIcons.OpenNew
                            ),
                            trailingContentData = ListItemTrailingContentDataUi.Icon(
                                iconData = AppIcons.KeyboardArrowRight
                            )
                        )
                    )
                )
            }

            if (changelogUrl != null) {
                add(
                    SettingsItemUi(
                        type = SettingsMenuItemType.CHANGELOG,
                        data = ListItemDataUi(
                            itemId = SettingsMenuItemType.CHANGELOG.itemId,
                            mainContentData = ListItemMainContentDataUi.Text(
                                text = resourceProvider.getString(
                                    R.string.settings_screen_option_changelog
                                )
                            ),
                            leadingContentData = ListItemLeadingContentDataUi.Icon(
                                iconData = AppIcons.OpenInBrowser
                            ),
                            trailingContentData = ListItemTrailingContentDataUi.Icon(
                                iconData = AppIcons.KeyboardArrowRight
                            )
                        )
                    )
                )
            }

            add(
                SettingsItemUi(
                    type = SettingsMenuItemType.RESET_WALLET,
                    data = ListItemDataUi(
                        itemId = SettingsMenuItemType.RESET_WALLET.itemId,
                        mainContentData = ListItemMainContentDataUi.Text(
                            text = resourceProvider.getString(
                                R.string.settings_screen_option_reset_wallet
                            )
                        ),
                        leadingContentData = ListItemLeadingContentDataUi.Icon(
                            iconData = AppIcons.Delete
                        ),
                        trailingContentData = ListItemTrailingContentDataUi.Icon(
                            iconData = AppIcons.KeyboardArrowRight
                        )
                    )
                )
            )
        }
    }

    override suspend fun toggleBiometricsAuthentication() {
        biometricInteractor.storeBiometricsUsageDecision(
            shouldUseBiometrics = !getBiometricUsageDecision()
        )
    }

    override suspend fun toggleShowBatchIssuanceCounter() {
        prefKeys.setShowBatchIssuanceCounter(
            value = !getShowBatchIssuanceCounter()
        )
    }

    override suspend fun toggleRegistrationCheck() {
        prefKeys.setRegistrationCheckEnabled(
            value = !getRegistrationCheckEnabled()
        )
    }

    override suspend fun toggleDCAPI() {
        dcapiSettingProvider.setEnabled(enabled = !dcapiSettingProvider.isEnabled())
        walletCoreDCAPIRegistrationController.registerCredentials()
    }

    private suspend fun getBiometricUsageDecision(): Boolean {
        return biometricInteractor.getBiometricUserSelection()
    }

    private suspend fun getShowBatchIssuanceCounter(): Boolean {
        return prefKeys.getShowBatchIssuanceCounter()
    }

    private suspend fun getRegistrationCheckEnabled(): Boolean {
        return prefKeys.getRegistrationCheckEnabled()
    }

    private fun deviceSupportsBiometrics(): Boolean {
        return when (biometricInteractor.getBiometricsAvailability()) {
            is BiometricsAvailability.CanAuthenticate,
            is BiometricsAvailability.NonEnrolled -> true

            is BiometricsAvailability.Failure -> false
        }
    }
}