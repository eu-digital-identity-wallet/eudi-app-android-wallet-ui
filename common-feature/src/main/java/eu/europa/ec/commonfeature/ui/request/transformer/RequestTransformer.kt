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

package eu.europa.ec.commonfeature.ui.request.transformer

import eu.europa.ec.commonfeature.ui.request.Event
import eu.europa.ec.commonfeature.ui.request.model.DocumentItemUi
import eu.europa.ec.commonfeature.ui.request.model.OptionalFieldItemUi
import eu.europa.ec.commonfeature.ui.request.model.RequestDataUi
import eu.europa.ec.commonfeature.ui.request.model.RequiredFieldsItemUi
import eu.europa.ec.commonfeature.ui.request.model.UserDataDomain
import eu.europa.ec.commonfeature.ui.request.model.UserIdentificationUi
import eu.europa.ec.commonfeature.ui.request.model.toUserIdentificationUi
import eu.europa.ec.commonfeature.ui.request.model.toUserIdentificationsUi

object RequestTransformer {

    fun transformToUiItems(
        userDataDomain: UserDataDomain,
    ): List<RequestDataUi<Event>> {
        val items = mutableListOf<RequestDataUi<Event>>()

        // Add document item.
        items += RequestDataUi.Document(
            documentItemUi = DocumentItemUi(
                title = userDataDomain.documentTypeUi.title
            )
        )
        items += RequestDataUi.Space()

        // Add optional field items.
        userDataDomain.optionalFields.forEachIndexed { itemIndex, userIdentificationDomain ->
            items += RequestDataUi.OptionalField(
                optionalFieldItemUi = OptionalFieldItemUi(
                    userIdentificationUi = userIdentificationDomain.toUserIdentificationUi(
                        id = itemIndex,
                        optional = true,
                        event = Event.UserIdentificationClicked(itemId = itemIndex)
                    )
                )
            )

            if (itemIndex != userDataDomain.optionalFields.lastIndex) {
                items += RequestDataUi.Space()
                items += RequestDataUi.Divider()
            }

            items += RequestDataUi.Space()
        }

        // Add required fields item.
        val requiredFieldsUi: List<UserIdentificationUi<Event>> =
            userDataDomain.requiredFields.toUserIdentificationsUi(
                optional = false,
                event = null
            )
        items += RequestDataUi.RequiredFields(
            requiredFieldsItemUi = RequiredFieldsItemUi(
                userIdentificationsUi = requiredFieldsUi,
                expanded = false,
                title = userDataDomain.requiredFieldsTitle,
                event = Event.ExpandOrCollapseRequiredDataList
            )
        )
        items += RequestDataUi.Space()

        return items
    }
}