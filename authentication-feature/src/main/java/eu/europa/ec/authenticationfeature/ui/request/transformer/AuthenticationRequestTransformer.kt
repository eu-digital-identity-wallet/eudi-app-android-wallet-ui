/*
 *
 *  * Copyright (c) 2023 European Commission
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package eu.europa.ec.authenticationfeature.ui.request.transformer

import eu.europa.ec.authenticationfeature.model.UserDataDomain
import eu.europa.ec.authenticationfeature.ui.request.Event
import eu.europa.ec.authenticationfeature.ui.request.model.AuthenticationRequestDataUi
import eu.europa.ec.authenticationfeature.ui.request.model.IdentificationItemUi
import eu.europa.ec.authenticationfeature.ui.request.model.OptionalFieldItemUi
import eu.europa.ec.authenticationfeature.ui.request.model.RequiredFieldsItemUi
import eu.europa.ec.authenticationfeature.ui.request.model.UserIdentificationUi
import eu.europa.ec.authenticationfeature.ui.request.model.toUserIdentificationUi
import eu.europa.ec.authenticationfeature.ui.request.model.toUserIdentificationsUi

object AuthenticationRequestTransformer {

    fun transformToUiItems(
        userDataDomain: UserDataDomain,
    ): List<AuthenticationRequestDataUi<Event>> {
        val items = mutableListOf<AuthenticationRequestDataUi<Event>>()

        // Add identification item.
        items += AuthenticationRequestDataUi.Identification(
            identificationItemUi = IdentificationItemUi(
                title = userDataDomain.identification.title
            )
        )
        items += AuthenticationRequestDataUi.Space()

        // Add optional field items.
        userDataDomain.optionalFields.forEachIndexed { itemIndex, userIdentificationDomain ->
            items += AuthenticationRequestDataUi.OptionalField(
                optionalFieldItemUi = OptionalFieldItemUi(
                    userIdentificationUi = userIdentificationDomain.toUserIdentificationUi(
                        id = itemIndex,
                        optional = true,
                        event = Event.UserIdentificationClicked(itemId = itemIndex)
                    )
                )
            )

            if (itemIndex != userDataDomain.optionalFields.lastIndex) {
                items += AuthenticationRequestDataUi.Space()
                items += AuthenticationRequestDataUi.Divider()
            }

            items += AuthenticationRequestDataUi.Space()
        }

        // Add required fields item.
        val requiredFieldsUi: List<UserIdentificationUi<Event>> =
            userDataDomain.requiredFields.toUserIdentificationsUi(
                optional = false,
                event = null
            )
        items += AuthenticationRequestDataUi.RequiredFields(
            requiredFieldsItemUi = RequiredFieldsItemUi(
                userIdentificationsUi = requiredFieldsUi,
                expanded = false,
                title = userDataDomain.requiredFieldsTitle,
                event = Event.ExpandOrCollapseRequiredDataList
            )
        )
        items += AuthenticationRequestDataUi.Space()

        return items
    }
}