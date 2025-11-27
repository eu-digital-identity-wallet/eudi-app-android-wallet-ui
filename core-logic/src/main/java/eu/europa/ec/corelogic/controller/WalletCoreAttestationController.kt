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

package eu.europa.ec.corelogic.controller

import eu.europa.ec.corelogic.config.WalletCoreConfig
import eu.europa.ec.eudi.openid4vci.Nonce
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
import org.multipaz.securearea.KeyInfo

interface WalletCoreAttestationController {

    suspend fun getWalletAttestation(
        keyInfo: KeyInfo
    ): Result<String>

    suspend fun getKeyAttestation(
        keys: List<KeyInfo>,
        nonce: Nonce?
    ): Result<String>
}

class WalletCoreAttestationControllerImpl(
    private val walletCoreConfig: WalletCoreConfig,
    private val httpClient: HttpClient
) : WalletCoreAttestationController {

    override suspend fun getWalletAttestation(
        keyInfo: KeyInfo
    ): Result<String> = runCatching {
        httpClient.use { client ->
            client.post("${walletCoreConfig.walletProviderHost}/wallet-instance-attestation/jwk") {
                contentType(ContentType.Application.Json)
                setBody(
                    buildJsonObject {
                        put("jwk", keyInfo.publicKey.toJwk())
                    }
                )
            }.bodyAsText()
                .let { Json.decodeFromString<JsonObject>(it) }
                .let { it.jsonObject["walletInstanceAttestation"]?.jsonPrimitive?.content }
                ?: throw IllegalStateException("No attestation response")
        }
    }

    override suspend fun getKeyAttestation(
        keys: List<KeyInfo>,
        nonce: Nonce?
    ): Result<String> = runCatching {
        httpClient.use { client ->
            client.post("${walletCoreConfig.walletProviderHost}/wallet-unit-attestation/jwk-set") {
                contentType(ContentType.Application.Json)
                setBody(
                    buildJsonObject {
                        put("nonce", JsonPrimitive(nonce?.value.orEmpty()))
                        putJsonObject("jwkSet") {
                            putJsonArray("keys") {
                                keys.forEach { keyInfo -> add(keyInfo.publicKey.toJwk()) }
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
}