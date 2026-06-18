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
import com.kss.euid.zk.sdk.zkContractV1
import kotlinx.datetime.LocalDate
import kotlinx.io.bytestring.ByteString
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.multipaz.cbor.Tstr
import org.multipaz.cbor.buildCborArray
import org.multipaz.cbor.toDataItem
import org.multipaz.cbor.toDataItemFullDate
import org.multipaz.cose.Cose
import org.multipaz.cose.CoseNumberLabel
import org.multipaz.cose.CoseSign1
import org.multipaz.crypto.EcPublicKeyDoubleCoordinate
import org.multipaz.crypto.X509Cert
import org.multipaz.crypto.X509CertChain
import org.multipaz.mdoc.devicesigned.DeviceAuth
import org.multipaz.mdoc.devicesigned.DeviceNamespaces
import org.multipaz.mdoc.issuersigned.IssuerNamespaces
import org.multipaz.mdoc.issuersigned.IssuerSignedItem
import org.multipaz.mdoc.response.MdocDocument
import org.multipaz.mdoc.zkp.ZkSystemSpec
import kotlin.time.Instant

/**
 * Mapping + full prove→verify round trip through [StwoZkSystem] on the host JVM, against a synthetic
 * PID [MdocDocument] fixture. The SDK prover/verifier are stubbed, so a passing round trip also proves
 * `forProver` and `forVerifier` build the byte-identical statement.
 */
class StwoZkSystemRoundTripTest {

    private val contract = zkContractV1()
    private val transcript = Tstr("session-transcript")
    private val timestamp = Instant.fromEpochSeconds(EPOCH_DAY * 86_400L)

    private fun pidSpec() = ZkSystemSpec(id = contract.specIdPid, system = contract.systemName).apply {
        addParam(contract.paramPredicateMode, "and")
        addParam(contract.paramMinAge, 18L)
        addParam(contract.paramAcceptedCountries, "300,196") // GR, CY
        addParam(contract.paramVersion, 1L)
        addParam(contract.paramNumAttributes, 2L)
    }

    private fun fixtureDocument(): MdocDocument {
        val certChain = X509CertChain(listOf(X509Cert.fromPem(ISSUER_CERT_PEM)))
        val issuerAuth = CoseSign1(
            protectedHeaders = mapOf(CoseNumberLabel(Cose.COSE_LABEL_ALG) to (-7L).toDataItem()), // ES256
            unprotectedHeaders = mapOf(CoseNumberLabel(Cose.COSE_LABEL_X5CHAIN) to certChain.toDataItem()),
            signature = ByteArray(64) { it.toByte() }, // dummy r||s; the stub prover ignores it
            payload = byteArrayOf(0xA1.toByte(), 0x00), // dummy MSO bytes (never decoded on our path)
        )
        val birthItem = IssuerSignedItem.fromValues(
            digestId = 0L,
            random = ByteString(ByteArray(16)),
            dataElementIdentifier = contract.elementBirthDate,
            dataElementValue = LocalDate.parse("1990-01-01").toDataItemFullDate(),
        )
        val natItem = IssuerSignedItem.fromValues(
            digestId = 1L,
            random = ByteString(ByteArray(16)),
            dataElementIdentifier = contract.elementNationality,
            dataElementValue = buildCborArray { add("GR"); add("CY") },
        )
        return MdocDocument(
            docType = contract.doctypePid,
            issuerAuth = issuerAuth,
            issuerNamespaces = IssuerNamespaces(
                data = mapOf(
                    contract.pidNamespace to mapOf(
                        contract.elementBirthDate to birthItem,
                        contract.elementNationality to natItem,
                    ),
                ),
            ),
            deviceAuth = DeviceAuth.Ecdsa(CoseSign1(emptyMap(), emptyMap(), ByteArray(64), null)),
            deviceNamespaces = DeviceNamespaces(emptyMap()),
            errors = emptyMap(),
        )
    }

    @Test
    fun forProver_extracts_issuer_key_and_predicate_params() {
        val statement = ZkPublicStatement.forProver(pidSpec(), fixtureDocument(), transcript, timestamp)
        val key = X509Cert.fromPem(ISSUER_CERT_PEM).ecPublicKey as EcPublicKeyDoubleCoordinate

        assertArrayEquals(key.x, statement.issuerKeyX)
        assertArrayEquals(key.y, statement.issuerKeyY)
        assertEquals(contract.doctypePid, statement.doctype)
        assertEquals(PredicateMode.AND, statement.predicateMode)
        assertEquals(18u, statement.ageThresholdYears)
        assertEquals(listOf(300u, 196u), statement.acceptedNumericCountries)
        assertEquals(NatMode.ANY, statement.natMode)
        assertEquals(EPOCH_DAY.toInt(), statement.todayEpochDay)
    }

    @Test
    fun witness_extracts_real_values() {
        val witness = ZkWitness.from(fixtureDocument())

        assertEquals(32, witness.issuerSigR.size)
        assertEquals(32, witness.issuerSigS.size)
        assertTrue("sig_structure should be reconstructed", witness.sigStructure.isNotEmpty())
        assertEquals("1990-01-01", witness.birthDate)
        assertEquals(listOf(300u, 196u), witness.nationalities) // GR, CY -> numeric
        assertEquals(
            setOf(contract.elementBirthDate, contract.elementNationality),
            witness.digestIds.keys,
        )
    }

    @Test
    fun generate_then_verify_round_trips() {
        val system = StwoZkSystem()
        val spec = pidSpec()
        val zkDocument = system.generateProof(spec, fixtureDocument(), transcript, timestamp)

        val results = zkDocument.documentData.issuerSigned[contract.pidNamespace]!!
        assertTrue("age_over_18 asserted", results.containsKey("age_over_18"))
        assertTrue("nationality_in_set asserted", results.containsKey(contract.resultNatInSet))

        // Throws ProofVerificationFailureException on mismatch; passing => forProver == forVerifier.
        system.verifyProof(zkDocument, spec, transcript)
    }

    private companion object {
        const val EPOCH_DAY = 19_000L

        // A throwaway self-signed P-256 cert (test only) — gives the fixture an issuer EC public key.
        val ISSUER_CERT_PEM = """
            -----BEGIN CERTIFICATE-----
            MIIBijCCAS+gAwIBAgIUD5W94iNIiz8ZdXt9Anu2EH94MM0wCgYIKoZIzj0EAwIw
            GjEYMBYGA1UEAwwPVGVzdCBQSUQgSXNzdWVyMB4XDTI2MDYxODEwMTkxNVoXDTM2
            MDYxNTEwMTkxNVowGjEYMBYGA1UEAwwPVGVzdCBQSUQgSXNzdWVyMFkwEwYHKoZI
            zj0CAQYIKoZIzj0DAQcDQgAErp+zN6paRhj8aMYknrg6M2gBjTxpkbyqmzd7hZeU
            o3F1Ke7zHzS8v0rJmTPVfT1MdUdMOaVQNAypYdv48ySe8KNTMFEwHQYDVR0OBBYE
            FB/exQsXrPp9tIgpN98cUD0mDksWMB8GA1UdIwQYMBaAFB/exQsXrPp9tIgpN98c
            UD0mDksWMA8GA1UdEwEB/wQFMAMBAf8wCgYIKoZIzj0EAwIDSQAwRgIhAM3OSldS
            0oAHH9uawA9jfayzSmwul9f0iAynDysnGKKpAiEAr5wj/3wf4Nc0yu9g12ukgMW7
            ZdHmLmHN9G4CRL5iKrU=
            -----END CERTIFICATE-----
        """.trimIndent()
    }
}
