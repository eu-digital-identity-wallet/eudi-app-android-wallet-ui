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

import eu.europa.ec.uilogic.component.utils.SPACING_MEDIUM

sealed interface AuthenticationRequestDataUi<T> {

    data class Identification<T>(
        val identificationItemUi: IdentificationItemUi
    ) : AuthenticationRequestDataUi<T>

    data class OptionalField<T>(
        val optionalFieldItemUi: OptionalFieldItemUi<T>
    ) : AuthenticationRequestDataUi<T>

    data class RequiredFields<T>(
        val requiredFieldsItemUi: RequiredFieldsItemUi<T>
    ) : AuthenticationRequestDataUi<T>

    data class Space<T>(
        val space: Int = SPACING_MEDIUM
    ) : AuthenticationRequestDataUi<T>

    data class Divider<T>(
        val width: Int = -1
    ) : AuthenticationRequestDataUi<T>
}