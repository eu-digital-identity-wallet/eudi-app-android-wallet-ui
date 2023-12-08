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
    DRIVING_LICENSE(codeName = "org.iso.18013.5.1"),
    DIGITAL_ID(codeName = "eu.europa.ec.eudiw.pid.1"),
    CONFERENCE_BADGE(codeName = "com.example.conference.badge"),
    OTHER(codeName = "")
}

fun String.toDocumentTypeUi(): DocumentTypeUi = when (this) {
    "eu.europa.ec.eudiw.pid.1" -> DocumentTypeUi.DIGITAL_ID
    "org.iso.18013.5.1.mDL" -> DocumentTypeUi.DRIVING_LICENSE
    "com.example.conference.badge" -> DocumentTypeUi.CONFERENCE_BADGE
    else -> DocumentTypeUi.OTHER
}