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

package eu.europa.ec.commonfeature.model

import eu.europa.ec.commonfeature.ui.document_details.model.DocumentDetailsUi
import eu.europa.ec.corelogic.model.DocumentIdentifier
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider

data class DocumentUi(
    val documentId: String,
    val documentName: String,
    val documentIdentifier: DocumentIdentifier,
    val documentExpirationDateFormatted: String,
    val documentHasExpired: Boolean,
    val documentImage: String,
    val documentDetails: List<DocumentDetailsUi>,
    val userFullName: String? = null,
)

fun DocumentIdentifier.toUiName(resourceProvider: ResourceProvider): String {
    return when (this) {
        is DocumentIdentifier.PID -> resourceProvider.getString(R.string.pid)
        is DocumentIdentifier.MDL -> resourceProvider.getString(R.string.mdl)
        is DocumentIdentifier.SAMPLE_DOCUMENTS -> resourceProvider.getString(R.string.load_sample_data)
        is DocumentIdentifier.OTHER -> docType
    }
}