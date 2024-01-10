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
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider

data class DocumentUi(
    val documentId: String,
    val documentName: String,
    val documentType: DocumentTypeUi,
    val documentExpirationDateFormatted: String,
    val documentImage: String,
    val documentDetails: List<DocumentDetailsUi>,
    val userFullName: String? = null,
)

enum class DocumentTypeUi(
    val codeName: String
) {
    PID(codeName = "eu.europa.ec.eudiw.pid.1"),
    MDL(codeName = "org.iso.18013.5.1"),
    CONFERENCE_BADGE(codeName = "com.example.conference.badge"),
    SAMPLE_DOCUMENTS(codeName = "load_sample_documents"),
    OTHER(codeName = "")
}

fun DocumentTypeUi.toUiName(resourceProvider: ResourceProvider): String {
    return when (this) {
        DocumentTypeUi.PID -> resourceProvider.getString(R.string.pid)
        DocumentTypeUi.MDL -> resourceProvider.getString(R.string.mdl)
        DocumentTypeUi.CONFERENCE_BADGE -> resourceProvider.getString(R.string.conference_badge)
        DocumentTypeUi.SAMPLE_DOCUMENTS -> resourceProvider.getString(R.string.load_sample_data)
        DocumentTypeUi.OTHER -> ""
    }
}

fun String.toDocumentTypeUi(): DocumentTypeUi = when (this) {
    "eu.europa.ec.eudiw.pid.1" -> DocumentTypeUi.PID
    "org.iso.18013.5.1.mDL" -> DocumentTypeUi.MDL
    "com.example.conference.badge" -> DocumentTypeUi.CONFERENCE_BADGE
    "load_sample_documents" -> DocumentTypeUi.SAMPLE_DOCUMENTS
    else -> DocumentTypeUi.OTHER
}