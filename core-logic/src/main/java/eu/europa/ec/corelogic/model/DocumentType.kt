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

package eu.europa.ec.corelogic.model

enum class DocumentType(
    val nameSpace: String,
    val docType: String
) {
    PID(
        nameSpace = "eu.europa.ec.eudiw.pid.1",
        docType = "eu.europa.ec.eudiw.pid.1"
    ),
    MDL(
        nameSpace = "org.iso.18013.5.1",
        docType = "org.iso.18013.5.1.mDL"
    ),
    CONFERENCE_BADGE(
        nameSpace = "com.example.conference.badge",
        docType = "com.example.conference.badge"
    ),
    SAMPLE_DOCUMENTS(
        nameSpace = "load_sample_documents",
        docType = "load_sample_documents"
    ),
    OTHER(nameSpace = "", docType = "");

    val isSupported: Boolean
        get() = when (this) {
            PID, MDL -> true
            CONFERENCE_BADGE, SAMPLE_DOCUMENTS, OTHER -> false
        }
}

fun String.toDocumentType(): DocumentType = when (this) {
    "eu.europa.ec.eudiw.pid.1" -> DocumentType.PID
    "org.iso.18013.5.1.mDL" -> DocumentType.MDL
    "com.example.conference.badge" -> DocumentType.CONFERENCE_BADGE
    "load_sample_documents" -> DocumentType.SAMPLE_DOCUMENTS
    else -> DocumentType.OTHER
}