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

import eu.europa.ec.uilogic.component.utils.SPACING_MEDIUM

sealed interface RequestDataUi<T> {

    data class Document<T>(
        val documentItemUi: DocumentItemUi
    ) : RequestDataUi<T>

    data class OptionalField<T>(
        val optionalFieldItemUi: OptionalFieldItemUi<T>
    ) : RequestDataUi<T>

    data class RequiredFields<T>(
        val requiredFieldsItemUi: RequiredFieldsItemUi<T>
    ) : RequestDataUi<T>

    data class Space<T>(
        val space: Int = SPACING_MEDIUM
    ) : RequestDataUi<T>

    data class Divider<T>(
        val width: Int = -1
    ) : RequestDataUi<T>
}