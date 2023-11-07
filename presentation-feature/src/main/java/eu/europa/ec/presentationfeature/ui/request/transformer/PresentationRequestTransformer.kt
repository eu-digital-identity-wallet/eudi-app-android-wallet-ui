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

package eu.europa.ec.presentationfeature.ui.request.transformer

import eu.europa.ec.presentationfeature.model.UserDataDomain
import eu.europa.ec.presentationfeature.ui.request.Event
import eu.europa.ec.presentationfeature.ui.request.model.DocumentItemUi
import eu.europa.ec.presentationfeature.ui.request.model.OptionalFieldItemUi
import eu.europa.ec.presentationfeature.ui.request.model.PresentationRequestDataUi
import eu.europa.ec.presentationfeature.ui.request.model.RequiredFieldsItemUi
import eu.europa.ec.presentationfeature.ui.request.model.UserIdentificationUi
import eu.europa.ec.presentationfeature.ui.request.model.toUserIdentificationUi
import eu.europa.ec.presentationfeature.ui.request.model.toUserIdentificationsUi

object PresentationRequestTransformer {

    fun transformToUiItems(
        userDataDomain: UserDataDomain,
    ): List<PresentationRequestDataUi<Event>> {
        val items = mutableListOf<PresentationRequestDataUi<Event>>()

        // Add document item.
        items += PresentationRequestDataUi.Document(
            documentItemUi = DocumentItemUi(
                title = userDataDomain.documentTypeUi.title
            )
        )
        items += PresentationRequestDataUi.Space()

        // Add optional field items.
        userDataDomain.optionalFields.forEachIndexed { itemIndex, userIdentificationDomain ->
            items += PresentationRequestDataUi.OptionalField(
                optionalFieldItemUi = OptionalFieldItemUi(
                    userIdentificationUi = userIdentificationDomain.toUserIdentificationUi(
                        id = itemIndex,
                        optional = true,
                        event = Event.UserIdentificationClicked(itemId = itemIndex)
                    )
                )
            )

            if (itemIndex != userDataDomain.optionalFields.lastIndex) {
                items += PresentationRequestDataUi.Space()
                items += PresentationRequestDataUi.Divider()
            }

            items += PresentationRequestDataUi.Space()
        }

        // Add required fields item.
        val requiredFieldsUi: List<UserIdentificationUi<Event>> =
            userDataDomain.requiredFields.toUserIdentificationsUi(
                optional = false,
                event = null
            )
        items += PresentationRequestDataUi.RequiredFields(
            requiredFieldsItemUi = RequiredFieldsItemUi(
                userIdentificationsUi = requiredFieldsUi,
                expanded = false,
                title = userDataDomain.requiredFieldsTitle,
                event = Event.ExpandOrCollapseRequiredDataList
            )
        )
        items += PresentationRequestDataUi.Space()

        return items
    }
}