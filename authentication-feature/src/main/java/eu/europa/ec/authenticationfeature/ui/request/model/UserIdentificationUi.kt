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

package eu.europa.ec.authenticationfeature.ui.request.model

import eu.europa.ec.authenticationfeature.model.UserIdentificationDomain

data class UserIdentificationUi<T>(
    val id: Int,
    val checked: Boolean,
    val enabled: Boolean,
    val userIdentificationDomain: UserIdentificationDomain,
    val event: T? = null
)

fun <T> UserIdentificationDomain.toUserIdentificationUi(
    id: Int,
    optional: Boolean,
    event: T?
): UserIdentificationUi<T> {
    return UserIdentificationUi(
        id = id,
        checked = true,
        enabled = optional,
        userIdentificationDomain = UserIdentificationDomain(
            name = this.name,
            value = this.value
        ),
        event = event
    )
}

fun <T> List<UserIdentificationDomain>.toUserIdentificationsUi(
    optional: Boolean,
    event: T?
): List<UserIdentificationUi<T>> {
    return this.mapIndexed { index, item ->
        item.toUserIdentificationUi(id = index, optional = optional, event = event)
    }
}