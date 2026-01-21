/*
 * Copyright (c) 2025 European Commission
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

package eu.europa.ec.dashboardfeature.util

object TestTag {

    object DashboardScreen {
        fun bottomNavigationItem(navItem: String) =
            "dashboard_screen_bottom_navigation_item_$navItem"
    }

    object DocumentsScreen {
        const val PLUS_BUTTON = "documents_screen_plus_button"
    }

    object DocumentDetailsScreen {
        const val DELETE_BUTTON = "document_details_screen_delete_button"
        const val BOTTOM_SHEET_DELETE_DOCUMENT_POSITIVE_BUTTON =
            "document_details_screen_dialogue_delete_document_positive_button"
        const val BOTTOM_SHEET_DELETE_DOCUMENT_NEGATIVE_BUTTON =
            "document_details_screen_dialogue_delete_document_negative_button"
    }
}