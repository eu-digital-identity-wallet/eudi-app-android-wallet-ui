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

package eu.europa.ec.dashboardfeature.ui.dashboard_new

import eu.europa.ec.dashboardfeature.interactor.DashboardInteractorNew
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.ListItemData
import eu.europa.ec.uilogic.component.ListItemLeadingContentData
import eu.europa.ec.uilogic.component.ListItemMainContentData
import eu.europa.ec.uilogic.component.ListItemTrailingContentData
import eu.europa.ec.uilogic.mvi.MviViewModel
import eu.europa.ec.uilogic.mvi.ViewEvent
import eu.europa.ec.uilogic.mvi.ViewSideEffect
import eu.europa.ec.uilogic.mvi.ViewState
import org.koin.android.annotation.KoinViewModel

data class State(

    // side menu
    val isSideMenuVisible: Boolean = false,
    val sideMenuTitle: String = "",
    val sideMenuOptions: List<ListItemData>
) : ViewState

sealed class Event : ViewEvent {
    data object Pop : Event()

    sealed class SideMenu : Event() {
        data object Show : SideMenu()
        data object Hide : SideMenu()
    }

    // side menu events
    data object OpenChangeQuickPin : Event()
}

sealed class Effect : ViewSideEffect {
    sealed class Navigation : Effect() {
        data object Pop : Navigation()
    }
}

@KoinViewModel
class DashboardViewModelNew(
    val interactor: DashboardInteractorNew,
    private val resourceProvider: ResourceProvider,
) : MviViewModel<Event, State, Effect>() {
    override fun setInitialState(): State {
        return State(
            sideMenuTitle = resourceProvider.getString(eu.europa.ec.resourceslogic.R.string.dashboard_side_menu_title),
            sideMenuOptions = listOf(
                ListItemData(
                    itemId = "changePinId",
                    mainContentData = ListItemMainContentData.Text(
                        text = resourceProvider.getString(eu.europa.ec.resourceslogic.R.string.dashboard_side_menu_change_pin)
                    ),
                    leadingContentData = ListItemLeadingContentData.Icon(
                        iconData = AppIcons.ChangePin
                    ),
                    trailingContentData = ListItemTrailingContentData.Icon(
                        iconData = AppIcons.KeyboardArrowRight
                    )
                )
            )
        )
    }

    override fun handleEvents(event: Event) {
        when (event) {
            Event.OpenChangeQuickPin -> {
                // TODO navigate to change pin
            }

            Event.Pop -> TODO()
            Event.SideMenu.Hide -> {
                setState { copy(isSideMenuVisible = false) }
            }

            Event.SideMenu.Show -> {
                setState { copy(isSideMenuVisible = true) }
            }
        }
    }
}