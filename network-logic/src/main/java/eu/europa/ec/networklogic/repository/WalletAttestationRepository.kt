/*
 * Copyright (c) 2025 European Commission
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

package eu.europa.ec.networklogic.repository

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

interface WalletAttestationRepository {

    suspend fun getWalletAttestation(
        baseUrl: String,
        keyInfo: JsonObject
    ): Result<String>

    suspend fun getKeyAttestation(
        baseUrl: String,
        keys: List<JsonObject>,
        nonce: String?
    ): Result<String>
}

class WalletAttestationRepositoryImpl(
    private val httpClient: HttpClient
) : WalletAttestationRepository {

    private companion object {
        const val WALLET_INSTANCE_ATTESTATION_PATH = "/wallet-instance-attestation/jwk"
        const val WALLET_UNIT_ATTESTATION_PATH = "/wallet-unit-attestation/jwk-set"
    }

    override suspend fun getWalletAttestation(
        baseUrl: String,
        keyInfo: JsonObject
    ): Result<String> = runCatching {
        httpClient.post(baseUrl + WALLET_INSTANCE_ATTESTATION_PATH) {
            contentType(ContentType.Application.Json)
            setBody(
                buildJsonObject {
                    put("jwk", keyInfo)
                }
            )
        }.bodyAsText()
            .let { Json.decodeFromString<JsonObject>(it) }
            .let { it.jsonObject["walletInstanceAttestation"]?.jsonPrimitive?.content }
            ?: throw IllegalStateException("No attestation response")
    }

    override suspend fun getKeyAttestation(
        baseUrl: String,
        keys: List<JsonObject>,
        nonce: String?
    ): Result<String> = runCatching {
        httpClient.post(baseUrl + WALLET_UNIT_ATTESTATION_PATH) {
            contentType(ContentType.Application.Json)
            setBody(
                buildJsonObject {
                    put("nonce", JsonPrimitive(nonce.orEmpty()))
                    putJsonObject("jwkSet") {
                        putJsonArray("keys") {
                            keys.forEach { keyInfo -> add(keyInfo) }
                        }
                    }
                }
            )
        }.bodyAsText()
            .let { Json.decodeFromString<JsonObject>(it) }
            .let { it.jsonObject["walletUnitAttestation"]?.jsonPrimitive?.content }
            ?: throw IllegalStateException("No attestation response")
    }
}