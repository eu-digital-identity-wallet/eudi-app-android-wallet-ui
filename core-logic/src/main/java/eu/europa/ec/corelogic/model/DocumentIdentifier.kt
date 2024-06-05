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

sealed interface DocumentIdentifier {
    val nameSpace: String
    val docType: String

    data object PID : DocumentIdentifier {
        override val nameSpace: String
            get() = "eu.europa.ec.eudiw.pid.1"
        override val docType: String
            get() = "eu.europa.ec.eudiw.pid.1"
    }

    data object MDL : DocumentIdentifier {
        override val nameSpace: String
            get() = "org.iso.18013.5.1"
        override val docType: String
            get() = "org.iso.18013.5.1.mDL"
    }

    data object SAMPLE : DocumentIdentifier {
        override val nameSpace: String
            get() = "load_sample_documents"
        override val docType: String
            get() = "load_sample_documents"
    }

    data class OTHER(
        override val nameSpace: String,
        override val docType: String,
    ) : DocumentIdentifier
}

fun DocumentIdentifier.isSupported(): Boolean {
    return when (this) {
        is DocumentIdentifier.PID, DocumentIdentifier.MDL -> true
        is DocumentIdentifier.SAMPLE, is DocumentIdentifier.OTHER -> false
    }
}

fun String.toDocumentIdentifier(): DocumentIdentifier = when (this) {
    "eu.europa.ec.eudiw.pid.1" -> DocumentIdentifier.PID
    "org.iso.18013.5.1.mDL" -> DocumentIdentifier.MDL
    "load_sample_documents" -> DocumentIdentifier.SAMPLE
    else -> DocumentIdentifier.OTHER(
        nameSpace = this,
        docType = this
    )
}