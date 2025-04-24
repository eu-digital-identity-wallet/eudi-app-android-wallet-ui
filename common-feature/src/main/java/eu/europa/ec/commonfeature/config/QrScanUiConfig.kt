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

package eu.europa.ec.commonfeature.config

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import eu.europa.ec.uilogic.serializer.UiSerializable
import eu.europa.ec.uilogic.serializer.UiSerializableParser
import eu.europa.ec.uilogic.serializer.adapter.SerializableTypeAdapter

sealed interface QrScanFlow {
    data object Presentation : QrScanFlow
    data class Issuance(val issuanceFlow: IssuanceFlowUiConfig) : QrScanFlow
    data object Signature : QrScanFlow
}

data class QrScanUiConfig(
    val title: String,
    val subTitle: String,
    val qrScanFlow: QrScanFlow
) : UiSerializable {

    companion object Parser : UiSerializableParser {
        override val serializedKeyName = "qrScanConfig"
        override fun provideParser(): Gson {
            return GsonBuilder().registerTypeAdapter(
                QrScanFlow::class.java,
                SerializableTypeAdapter<QrScanFlow>()
            ).create()
        }
    }
}