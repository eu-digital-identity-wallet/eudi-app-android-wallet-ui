/*
 * Copyright (c) 2023 European Commission
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.europa.ec.eudi.wallet.ui.detail

import android.content.Context
import android.graphics.BitmapFactory
import eu.europa.ec.eudi.wallet.document.Constants.EU_PID_DOCTYPE
import eu.europa.ec.eudi.wallet.document.Constants.EU_PID_NAMESPACE
import eu.europa.ec.eudi.wallet.document.Constants.MDL_DOCTYPE
import eu.europa.ec.eudi.wallet.document.Constants.MDL_NAMESPACE
import eu.europa.ec.eudi.wallet.util.CBOR

class DocumentDataElementParser(val context: Context) {

    fun read(docType: String, elements: Map<String, Map<String, ByteArray>>): DocumentDetails {
        val builder = StringBuilder()
        var portraitBytes: ByteArray? = null
        var signatureBytes: ByteArray? = null
        elements.keys.forEach { ns ->
            builder.append("<br>")
            builder.append("<h5>Namespace: $ns</h5>")
            builder.append("<p>")
            elements[ns]?.keys?.forEach { entryName ->
                val byteArray: ByteArray? = elements[ns]?.get(entryName)
                byteArray?.let { value ->
                    val valueStr: String
                    if (isPortraitElement(docType, ns, entryName)) {
                        valueStr = String.format("(%d bytes, shown above)", value.size)
                        portraitBytes = CBOR.cborDecodeByteString(value)
                    } else if (isSignatureElement(docType, ns, entryName)) {
                        valueStr = String.format("(%d bytes, shown above)", value.size)
                        signatureBytes = CBOR.cborDecodeByteString(value)
                    } else {
                        valueStr = CBOR.cborPrettyPrint(value)
                    }
                    builder.append("<b>${stringValueFor(entryName)}</b> -> $valueStr<br>")
                }
            }
            builder.append("</p><br>")
        }
        val portrait = portraitBytes?.let { bytes ->
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }
        val signature = signatureBytes?.let { bytes ->
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }
        return DocumentDetails(builder.toString(), portrait = portrait, signature = signature)
    }

    private fun isPortraitElement(
        docType: String,
        namespace: String?,
        entryName: String?
    ): Boolean {
        return (docType == MDL_DOCTYPE || docType == EU_PID_DOCTYPE)
                && (namespace == MDL_NAMESPACE || namespace == EU_PID_NAMESPACE)
                && entryName == "portrait"
    }

    private fun isSignatureElement(
        docType: String,
        namespace: String?,
        entryName: String?
    ): Boolean {
        return (docType == MDL_DOCTYPE || docType == EU_PID_DOCTYPE)
                && (namespace == MDL_NAMESPACE || namespace == EU_PID_NAMESPACE)
                && entryName == "signature_usual_mark"
    }

    private fun stringValueFor(element: String): String {
        val identifier = context.resources.getIdentifier(element, "string", context.packageName)
        return if (identifier != 0) context.getString(identifier) else element
    }
}