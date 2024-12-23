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

import eu.europa.ec.eudi.wallet.document.Document
import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcFormat

typealias FormatType = String

sealed interface DocumentIdentifier {
    val formatType: FormatType

    data object MdocPid : DocumentIdentifier {
        override val formatType: FormatType
            get() = "eu.europa.ec.eudi.pid.1"
    }

    data object MdocPseudonym : DocumentIdentifier {
        override val formatType: FormatType
            get() = "eu.europa.ec.eudi.pseudonym.age_over_18.1"
    }

    data object SdJwtPid : DocumentIdentifier {
        override val formatType: FormatType
            get() = "urn:eu:europa:ec:eudi:pid:1"
    }

    data class OTHER(
        override val formatType: FormatType,
    ) : DocumentIdentifier
}

/**
 * @return A [DocumentIdentifier] from a FormatType.
 */
fun FormatType.toDocumentIdentifier(): DocumentIdentifier = when (this) {
    DocumentIdentifier.MdocPid.formatType -> DocumentIdentifier.MdocPid
    DocumentIdentifier.SdJwtPid.formatType -> DocumentIdentifier.SdJwtPid
    else -> DocumentIdentifier.OTHER(formatType = this)
}

fun Document.toDocumentIdentifier(): DocumentIdentifier {
    val formatType = when (val f = format) {
        is MsoMdocFormat -> f.docType
        is SdJwtVcFormat -> f.vct
    }
    return createDocumentIdentifier(formatType)
}

private fun createDocumentIdentifier(
    formatType: FormatType
): DocumentIdentifier {
    return when (formatType) {
        DocumentIdentifier.MdocPid.formatType -> DocumentIdentifier.MdocPid
        DocumentIdentifier.SdJwtPid.formatType -> DocumentIdentifier.SdJwtPid
        else -> DocumentIdentifier.OTHER(formatType = formatType)
    }
}