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

import eu.europa.ec.authenticationlogic.controller.session.SessionController
import eu.europa.ec.corelogic.worker.WalletWorkScheduler
import eu.europa.ec.dashboardfeature.ui.dashboard.model.SideMenuItemUi
import eu.europa.ec.dashboardfeature.ui.dashboard.model.SideMenuTypeUi
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.ListItemDataUi
import eu.europa.ec.uilogic.component.ListItemLeadingContentDataUi
import eu.europa.ec.uilogic.component.ListItemMainContentDataUi
import eu.europa.ec.uilogic.component.ListItemTrailingContentDataUi

interface DashboardInteractor {
    fun getSideMenuOptions(): List<SideMenuItemUi>

    /**
     * Schedules the wallet's background work once the user has logged in: the periodic revocation
     * and re-issuance checks, and the revocation check that runs once per process. Does nothing
     * while the wallet is locked, as a dashboard restored before login is. So the start-up check
     * waits for login, and on a new install nothing is scheduled before the first login. Periodic
     * work already scheduled is kept, and runs on WorkManager's schedule whether or not the wallet
     * is unlocked. Unlike the factory reset, which schedules only the periodic checks, this also
     * asks for the start-up check. Safe to call on every visit: it runs once per process.
     */
    fun scheduleBackgroundWork()
}

class DashboardInteractorImpl(
    private val resourceProvider: ResourceProvider,
    private val walletWorkScheduler: WalletWorkScheduler,
    private val sessionController: SessionController,
) : DashboardInteractor {

    override fun getSideMenuOptions(): List<SideMenuItemUi> {
        return buildList {
            add(
                SideMenuItemUi(
                    type = SideMenuTypeUi.CHANGE_PIN,
                    data = ListItemDataUi(
                        itemId = SideMenuTypeUi.CHANGE_PIN.itemId,
                        mainContentData = ListItemMainContentDataUi.Text(
                            text = resourceProvider.getString(R.string.dashboard_side_menu_option_change_pin)
                        ),
                        leadingContentData = ListItemLeadingContentDataUi.Icon(
                            iconData = AppIcons.ChangePin
                        ),
                        trailingContentData = ListItemTrailingContentDataUi.Icon(
                            iconData = AppIcons.KeyboardArrowRight
                        )
                    )
                )
            )

            add(
                SideMenuItemUi(
                    type = SideMenuTypeUi.SETTINGS,
                    data = ListItemDataUi(
                        itemId = SideMenuTypeUi.SETTINGS.itemId,
                        mainContentData = ListItemMainContentDataUi.Text(
                            text = resourceProvider.getString(R.string.dashboard_side_menu_option_settings)
                        ),
                        leadingContentData = ListItemLeadingContentDataUi.Icon(
                            iconData = AppIcons.Settings
                        ),
                        trailingContentData = ListItemTrailingContentDataUi.Icon(
                            iconData = AppIcons.KeyboardArrowRight
                        )
                    )
                )
            )

            add(
                SideMenuItemUi(
                    type = SideMenuTypeUi.ABOUT,
                    data = ListItemDataUi(
                        itemId = SideMenuTypeUi.ABOUT.itemId,
                        mainContentData = ListItemMainContentDataUi.Text(
                            text = resourceProvider.getString(R.string.trust_mark_about_title)
                        ),
                        leadingContentData = ListItemLeadingContentDataUi.Icon(
                            iconData = AppIcons.Info
                        ),
                        trailingContentData = ListItemTrailingContentDataUi.Icon(
                            iconData = AppIcons.KeyboardArrowRight
                        ),
                    ),
                )
            )
        }
    }

    override fun scheduleBackgroundWork() {
        if (!sessionController.isUnlocked) return
        walletWorkScheduler.schedule()
        walletWorkScheduler.scheduleStartupCheck()
    }
}