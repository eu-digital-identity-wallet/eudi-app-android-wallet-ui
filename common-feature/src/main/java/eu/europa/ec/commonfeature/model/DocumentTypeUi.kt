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

data class DocumentUi(
    val documentId: String,
    val documentType: DocumentTypeUi,
    val documentStatus: DocumentStatusUi,
    val documentImage: String,
    val documentItems: List<DocumentItemUi>
)

data class DocumentItemUi(
    val title: String,
    val value: String
)

enum class DocumentTypeUi(
    val title: String
) {
    DRIVING_LICENSE(title = "Driving License"),
    DIGITAL_ID(title = "Digital ID"),
    OTHER(title = "Other document")
}

fun String.toDocumentTypeUi(): DocumentTypeUi = when(this){
    "eu.europa.ec.eudiw.pid.1" -> DocumentTypeUi.DIGITAL_ID
    "org.iso.18013.5.1.mDL" -> DocumentTypeUi.DRIVING_LICENSE
    else -> DocumentTypeUi.OTHER
}

enum class DocumentStatusUi(
    val title: String
) {
    ACTIVE(title = "Active"),
    INACTIVE(title = "Inactive")
}