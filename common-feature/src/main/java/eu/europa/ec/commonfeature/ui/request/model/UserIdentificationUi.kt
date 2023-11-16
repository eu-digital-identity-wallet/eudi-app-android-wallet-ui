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

package eu.europa.ec.commonfeature.ui.request.model

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