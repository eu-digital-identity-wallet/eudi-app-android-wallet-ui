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

package eu.europa.ec.commonfeature.util

import eu.europa.ec.businesslogic.util.getStringFromJsonOrEmpty
import eu.europa.ec.commonfeature.model.toDocumentTypeUi
import eu.europa.ec.commonfeature.ui.document_details.model.DocumentJsonKeys
import eu.europa.ec.eudi.wallet.document.Document
import eu.europa.ec.eudi.wallet.document.nameSpacedDataJSONObject
import org.json.JSONObject

fun extractValueFromDocumentOrEmpty(
    document: Document,
    key: String
): String {
    val docType = document.docType.toDocumentTypeUi()
    val documentJsonObject =
        document.nameSpacedDataJSONObject.get(docType.codeName) as? JSONObject
    return documentJsonObject?.getStringFromJsonOrEmpty(key) ?: ""
}

fun extractFullNameFromDocumentOrEmpty(document: Document): String {
    val firstName = extractValueFromDocumentOrEmpty(
        document = document,
        key = DocumentJsonKeys.FIRST_NAME
    )
    val lastName = extractValueFromDocumentOrEmpty(
        document = document,
        key = DocumentJsonKeys.LAST_NAME
    )
    return "$firstName $lastName"
}