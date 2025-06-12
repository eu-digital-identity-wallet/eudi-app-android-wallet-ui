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

package eu.europa.ec.dashboardfeature.interactor

import android.net.Uri
import eu.europa.ec.businesslogic.config.ConfigLogic
import eu.europa.ec.businesslogic.controller.log.LogController
import eu.europa.ec.businesslogic.controller.storage.PrefKeys
import eu.europa.ec.dashboardfeature.ui.settings.model.SettingsItemUi
import eu.europa.ec.dashboardfeature.ui.settings.model.SettingsMenuItemType
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.ListItemData
import eu.europa.ec.uilogic.component.ListItemLeadingContentData
import eu.europa.ec.uilogic.component.ListItemMainContentData
import eu.europa.ec.uilogic.component.ListItemTrailingContentData
import eu.europa.ec.uilogic.component.wrap.SwitchData

interface SettingsInteractor {
    fun getAppVersion(): String
    fun getChangelogUrl(): String?
    fun retrieveLogFileUris(): ArrayList<Uri>
    fun getSettingsItemsUi(changelogUrl: String?): List<SettingsItemUi>
    fun getShowBatchIssuanceCounter(): Boolean
    fun toggleShowBatchIssuanceCounter()
}

class SettingsInteractorImpl(
    private val configLogic: ConfigLogic,
    private val logController: LogController,
    private val resourceProvider: ResourceProvider,
    private val prefKeys: PrefKeys,
) : SettingsInteractor {

    override fun getAppVersion(): String = configLogic.appVersion

    override fun getChangelogUrl(): String? = configLogic.changelogUrl

    override fun retrieveLogFileUris(): ArrayList<Uri> {
        return ArrayList(logController.retrieveLogFileUris())
    }

    override fun getSettingsItemsUi(changelogUrl: String?): List<SettingsItemUi> {
        return buildList<SettingsItemUi> {
            add(
                SettingsItemUi(
                    type = SettingsMenuItemType.SHOW_BATCH_ISSUANCE_COUNTER,
                    data = ListItemData(
                        itemId = resourceProvider.getString(R.string.settings_screen_option_show_batch_issuance_counter_id),
                        mainContentData = ListItemMainContentData.Text(
                            text = resourceProvider.getString(R.string.settings_screen_option_show_batch_issuance_counter)
                        ),
                        trailingContentData = ListItemTrailingContentData.Switch(
                            switchData = SwitchData(
                                isChecked = getCurrentShowBatchIssuanceCounter(),
                                enabled = true,
                            )
                        )
                    )
                )
            )

            add(
                SettingsItemUi(
                    type = SettingsMenuItemType.RETRIEVE_LOGS,
                    data = ListItemData(
                        itemId = resourceProvider.getString(R.string.settings_screen_option_retrieve_logs_id),
                        mainContentData = ListItemMainContentData.Text(
                            text = resourceProvider.getString(R.string.settings_screen_option_retrieve_logs)
                        ),
                        leadingContentData = ListItemLeadingContentData.Icon(
                            iconData = AppIcons.OpenNew
                        ),
                        trailingContentData = ListItemTrailingContentData.Icon(
                            iconData = AppIcons.KeyboardArrowRight
                        )
                    )
                )
            )

            if (changelogUrl != null) {
                add(
                    SettingsItemUi(
                        type = SettingsMenuItemType.CHANGELOG,
                        data = ListItemData(
                            itemId = resourceProvider.getString(R.string.settings_screen_option_changelog_id),
                            mainContentData = ListItemMainContentData.Text(
                                text = resourceProvider.getString(R.string.settings_screen_option_changelog)
                            ),
                            leadingContentData = ListItemLeadingContentData.Icon(
                                iconData = AppIcons.OpenInBrowser
                            ),
                            trailingContentData = ListItemTrailingContentData.Icon(
                                iconData = AppIcons.KeyboardArrowRight
                            )
                        )
                    )
                )
            }
        }
    }

    override fun getShowBatchIssuanceCounter(): Boolean {
        return getCurrentShowBatchIssuanceCounter()
    }

    override fun toggleShowBatchIssuanceCounter() {
        prefKeys.setShowBatchIssuanceCounter(
            value = !getCurrentShowBatchIssuanceCounter()
        )
    }

    private fun getCurrentShowBatchIssuanceCounter(): Boolean {
        return prefKeys.getShowBatchIssuanceCounter()
    }
}