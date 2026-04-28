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

package eu.europa.ec.uilogic.navigation.helper

import android.content.Intent
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

const val INTENT_ACTION_KEY = "intent_action"

@Parcelize
data class IntentAction(
    val intent: Intent,
    val type: IntentType
) : Parcelable

enum class IntentType(val associatedActions: List<String>) {
    DC_API(
        associatedActions = listOf(
            "androidx.identitycredentials.action.get_credentials",
            "androidx.credentials.registry.provider.action.get_credential"
        )
    ),
}

