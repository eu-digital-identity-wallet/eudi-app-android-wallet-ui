/*
 * Copyright (c) 2026 European Commission
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

package eu.europa.ec.zkplogic

import com.kss.euid.zk.sdk.NatMode
import com.kss.euid.zk.sdk.PredicateMode
import com.kss.euid.zk.sdk.ZkPublicStatement
import com.kss.euid.zk.sdk.ZkWitness
import com.kss.euid.zk.sdk.isoAlpha2ToNumeric
import com.kss.euid.zk.sdk.predicateModeFromToken
import com.kss.euid.zk.sdk.predicateModeUsesAge
import com.kss.euid.zk.sdk.predicateModeUsesNat
import com.kss.euid.zk.sdk.resultAgeOver
import kotlinx.io.bytestring.ByteString
import org.multipaz.cbor.Bstr
import org.multipaz.cbor.Cbor
import org.multipaz.cbor.DataItem
import org.multipaz.cbor.Tagged
import org.multipaz.cbor.buildCborArray
import org.multipaz.cbor.buildCborMap
import org.multipaz.cbor.toDataItem
import org.multipaz.cose.CoseSign1
import org.multipaz.crypto.EcPublicKey
import org.multipaz.crypto.EcPublicKeyDoubleCoordinate
import org.multipaz.mdoc.issuersigned.IssuerSignedItem
import org.multipaz.mdoc.response.MdocDocument
import org.multipaz.mdoc.zkp.ZkDocument
import org.multipaz.mdoc.zkp.ZkDocumentData
import org.multipaz.mdoc.zkp.ZkSystemSpec
import kotlin.time.Instant

/** Public statement at proving time — `today` derives from the proof [timestamp] (device clock) TODO. */
fun ZkPublicStatement.Companion.forProver(
    spec: ZkSystemSpec,
    document: MdocDocument,
    sessionTranscript: DataItem,
    timestamp: Instant
): ZkPublicStatement {
    val (keyX, keyY) = document.issuerCertChain.certificates.first().ecPublicKey.toXY()

    val mode = PredicateMode.from(spec)
    val minAge = spec.getParam<Long>(ZK_CONTRACT.paramMinAge)
    val accepted = spec.getParam<String>(ZK_CONTRACT.paramAcceptedCountries)
        ?.split(",")
        ?.mapNotNull { it.trim().toUIntOrNull() }

    return ZkPublicStatement(
        specId = spec.id,
        version = (spec.getParam<Long>(ZK_CONTRACT.paramVersion) ?: 1L).toUInt(),
        doctype = ZK_CONTRACT.doctypePid,
        namespace = ZK_CONTRACT.pidNamespace,
        issuerKeyX = keyX,
        issuerKeyY = keyY,
        todayEpochDay = timestamp.epochDay(), // TODO maybe not use system clock
        nonce = Cbor.encode(sessionTranscript),
        predicateMode = mode,
        ageThresholdYears = if (predicateModeUsesAge(mode)) minAge?.toUInt() else null,
        acceptedNumericCountries = if (predicateModeUsesNat(mode)) accepted else null,
        natMode = NatMode.ANY, // only "any" supported this iteration
    )
}

/** Public statement at verify time — issuer key + `today` come from the proof's [ZkDocument]. */
fun ZkPublicStatement.Companion.forVerifier(
    spec: ZkSystemSpec,
    zkDocument: ZkDocument,
    sessionTranscript: DataItem,
): ZkPublicStatement {
    val chain = zkDocument.documentData.msoX5chain
        ?: throw IllegalArgumentException("ZkDocument is missing msoX5chain (issuer key)")
    val (keyX, keyY) = chain.certificates.first().ecPublicKey.toXY()
    val mode = PredicateMode.from(spec)
    val minAge = spec.getParam<Long>(ZK_CONTRACT.paramMinAge)
    val accepted = spec.getParam<String>(ZK_CONTRACT.paramAcceptedCountries)
        ?.split(",")
        ?.mapNotNull { it.trim().toUIntOrNull() }

    return ZkPublicStatement(
        specId = spec.id,
        version = (spec.getParam<Long>(ZK_CONTRACT.paramVersion) ?: 1L).toUInt(),
        doctype = ZK_CONTRACT.doctypePid,
        namespace = ZK_CONTRACT.pidNamespace,
        issuerKeyX = keyX,
        issuerKeyY = keyY,
        todayEpochDay = zkDocument.documentData.timestamp.epochDay(), // TODO
        nonce = Cbor.encode(sessionTranscript),
        predicateMode = mode,
        ageThresholdYears = if (predicateModeUsesAge(mode)) minAge?.toUInt() else null,
        acceptedNumericCountries = if (predicateModeUsesNat(mode)) accepted else null,
        natMode = NatMode.ANY, // only "any" supported this iteration
    )
}

/** Witness (prove side only) — real values extracted from the credential. */
fun ZkWitness.Companion.from(
    document: MdocDocument,
): ZkWitness {
    val sig = document.issuerAuth.signature // ES256 raw r||s
    val half = sig.size / 2
    val items = document.issuerNamespaces.data[ZK_CONTRACT.pidNamespace].orEmpty()
    val birthItem = items[ZK_CONTRACT.elementBirthDate]
    val natItem = items[ZK_CONTRACT.elementNationality]

    return ZkWitness(
        issuerSigR = if (sig.isNotEmpty()) sig.copyOfRange(0, half) else ByteArray(0),
        issuerSigS = if (sig.isNotEmpty()) sig.copyOfRange(half, sig.size) else ByteArray(0),
        sigStructure = document.issuerAuth.cborEncode(),
        mso = document.issuerAuth.payload ?: ByteArray(0),
        birthDateItem = birthItem?.cborEncode() ?: ByteArray(0),
        nationalityItem = natItem?.cborEncode() ?: ByteArray(0),
        birthDate = birthItem?.asDateString().orEmpty(),
        nationalities = natItem?.asNumericNationalities().orEmpty(),
        digestIds = buildMap {
            birthItem?.let { put(ZK_CONTRACT.elementBirthDate, it.digestId.toUInt()) }
            natItem?.let { put(ZK_CONTRACT.elementNationality, it.digestId.toUInt()) }
        },
    )
}

/** Wraps the proof + asserted boolean results into a Multipaz [ZkDocument]. */
fun ZkDocument.Companion.from(
    spec: ZkSystemSpec,
    document: MdocDocument,
    proof: ByteArray,
    timestamp: Instant,
): ZkDocument {
    val mode = PredicateMode.from(spec)
    val minAge = spec.getParam<Long>(ZK_CONTRACT.paramMinAge)

    val resultClaims = buildMap<String, DataItem> {
        if (predicateModeUsesAge(mode) && minAge != null) {
            put(resultAgeOver(minAge.toUInt()), true.toDataItem())
        }
        if (predicateModeUsesNat(mode)) {
            put(ZK_CONTRACT.resultNatInSet, true.toDataItem())
        }
    }

    val data = ZkDocumentData(
        zkSystemSpecId = spec.id,
        docType = document.docType,
        timestamp = timestamp,
        issuerSigned = mapOf(ZK_CONTRACT.pidNamespace to resultClaims),
        deviceSigned = emptyMap(),
        msoX5chain = document.issuerCertChain,
    )
    return ZkDocument(documentData = data, proof = ByteString(proof))
}

private fun EcPublicKey.toXY(): Pair<ByteArray, ByteArray> = when (this) {
    is EcPublicKeyDoubleCoordinate -> this.x to this.y
    else -> throw IllegalArgumentException("Expected a P-256 double-coordinate issuer key")
}

/** Days since 1970-01-01 (UTC) from an [Instant]. */
private fun Instant.epochDay(): Int = (this.epochSeconds / 86_400L).toInt()

private fun PredicateMode.Companion.from(spec: ZkSystemSpec) =
    spec.getParam<String>(ZK_CONTRACT.paramPredicateMode)
        ?.let { predicateModeFromToken(it) }
        ?: PredicateMode.AND

/**
 * The COSE `Sig_structure` (ToBeSigned) for the issuer's `COSE_Sign1`. Mirrors Multipaz's internal
 * `coseBuildToBeSigned` byte-for-byte: `["Signature1", protected, external_aad(empty), payload]`,
 * where `protected` is the encoded protected-header map (empty bstr if none). `SHA256(this)` is the
 * message the issuer signed.
 */
private fun CoseSign1.cborEncode(): ByteArray {
    val protected = if (this.protectedHeaders.isNotEmpty()) {
        Cbor.encode(buildCborMap {
            this@cborEncode.protectedHeaders.forEach { (l, di) ->
                put(
                    l.toDataItem(),
                    di
                )
            }
        })
    } else {
        ByteArray(0)
    }
    return Cbor.encode(
        buildCborArray {
            add("Signature1")
            add(protected)
            add(ByteArray(0)) // external_aad
            add(this@cborEncode.payload ?: ByteArray(0))
        }
    )
}

/** `IssuerSignedItemBytes` = `#6.24(bstr .cbor IssuerSignedItem)`; `SHA256(this)` is the MSO digest. */
private fun IssuerSignedItem.cborEncode(): ByteArray =
    Cbor.encode(Tagged(Tagged.ENCODED_CBOR, Bstr(Cbor.encode(this.dataItem))))

/** mdoc `nationality` is an array of ISO alpha-2 strings; map to numeric via the SDK. */
private fun IssuerSignedItem.asNumericNationalities(): List<UInt> =
    this.dataElementValue.asArray.mapNotNull { isoAlpha2ToNumeric(it.asTstr) }

/** Extracts the date string from a full-date/tdate (`tag 1004`/`tag 0`) or a plain text value. */
private fun IssuerSignedItem.asDateString(): String =
    this.dataElementValue.let { if (it is Tagged) it.asTagged.asTstr else it.asTstr }