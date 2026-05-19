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
import eu.europa.ec.commonfeature.interactor.BiometricInteractor
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
}

class SettingsInteractorImpl(
    private val biometricInteractor: BiometricInteractor,
    private val configLogic: ConfigLogic,
    private val logController: LogController,
    private val resourceProvider: ResourceProvider,
) : SettingsInteractor,
    BiometricInteractor by biometricInteractor {

    override fun getAppVersion(): String = configLogic.appVersion

    override fun getChangelogUrl(): String? = configLogic.changelogUrl

    override fun retrieveLogFileUris(): ArrayList<Uri> {
        return ArrayList(logController.retrieveLogFileUris())
    }

    override suspend fun getSettingsItemsUi(changelogUrl: String?): List<SettingsItemUi> {
        val deviceSupportsBiometrics = deviceSupportsBiometrics()

        return buildList {
            if (deviceSupportsBiometrics) {
                add(
                    SettingsItemUi(
                        type = SettingsMenuItemType.BIOMETRICS_AUTHENTICATION,
                        data = ListItemDataUi(
                            itemId = SettingsMenuItemType.BIOMETRICS_AUTHENTICATION.itemId,
                            mainContentData = ListItemMainContentDataUi.Text(
                                text = resourceProvider.getString(R.string.settings_screen_option_biometrics_authentication)
                            ),
                            leadingContentData = ListItemLeadingContentDataUi.Icon(
                                iconData = AppIcons.TouchId
                            ),
                            trailingContentData = ListItemTrailingContentDataUi.Switch(
                                switchData = SwitchDataUi(
                                    isChecked = biometricInteractor.getBiometricUserSelection(),
                                    enabled = true,
                                )
                            )
                        )
                    )
                )
            }

            add(
                SettingsItemUi(
                    type = SettingsMenuItemType.RETRIEVE_LOGS,
                    data = ListItemDataUi(
                        itemId = SettingsMenuItemType.RETRIEVE_LOGS.itemId,
                        mainContentData = ListItemMainContentDataUi.Text(
                            text = resourceProvider.getString(R.string.settings_screen_option_retrieve_logs)
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

            if (changelogUrl != null) {
                add(
                    SettingsItemUi(
                        type = SettingsMenuItemType.CHANGELOG,
                        data = ListItemDataUi(
                            itemId = SettingsMenuItemType.CHANGELOG.itemId,
                            mainContentData = ListItemMainContentDataUi.Text(
                                text = resourceProvider.getString(R.string.settings_screen_option_changelog)
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
        }
    }

    override suspend fun toggleBiometricsAuthentication() {
        biometricInteractor.storeBiometricsUsageDecision(
            shouldUseBiometrics = !biometricInteractor.getBiometricUserSelection()
        )
    }

    private fun deviceSupportsBiometrics(): Boolean {
        return when (biometricInteractor.getBiometricsAvailability()) {
            is BiometricsAvailability.CanAuthenticate,
            is BiometricsAvailability.NonEnrolled -> true

            is BiometricsAvailability.Failure -> false
        }
    }
}