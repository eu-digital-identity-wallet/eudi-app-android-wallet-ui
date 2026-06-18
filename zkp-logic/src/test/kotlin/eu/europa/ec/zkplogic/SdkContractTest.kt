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
import com.kss.euid.zk.sdk.proveIdentity
import com.kss.euid.zk.sdk.verifyIdentity
import com.kss.euid.zk.sdk.zkContractV1
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Exercises the STWO SDK directly on the host JVM via the `eu-id-zk-sdk-jvm` fat jar (JNA loads the
 * desktop native — no emulator). Proves the FFI surface + the stubbed prove/verify round trip; uses no
 * Multipaz / Android types.
 */
class SdkContractTest {

    private fun sampleStatement(ageThreshold: UInt? = 18u) = ZkPublicStatement(
        specId = "stwo-euid-pid-v1",
        version = 1u,
        doctype = "eu.europa.ec.eudi.pid.1",
        namespace = "eu.europa.ec.eudi.pid.1",
        issuerKeyX = ByteArray(32) { 0x11 },
        issuerKeyY = ByteArray(32) { 0x22 },
        todayEpochDay = 7305,
        nonce = byteArrayOf(0xA, 0xB, 0xC),
        predicateMode = PredicateMode.AND,
        ageThresholdYears = ageThreshold,
        acceptedNumericCountries = listOf(56u, 196u, 300u),
        natMode = NatMode.ANY,
    )

    private fun sampleWitness() = ZkWitness(
        issuerSigR = ByteArray(32) { 1 },
        issuerSigS = ByteArray(32) { 2 },
        sigStructure = ByteArray(16) { 3 },
        mso = ByteArray(16) { 4 },
        birthDateItem = ByteArray(8) { 5 },
        nationalityItem = ByteArray(8) { 6 },
        birthDate = "1990-01-01",
        nationalities = listOf(300u),
        digestIds = mapOf("birth_date" to 0u, "nationality" to 1u),
    )

    @Test
    fun contract_exposes_stable_identifiers() {
        val c = zkContractV1()
        assertEquals("stwo-euid-v1", c.systemName)
        assertEquals("stwo-euid-pid-v1", c.specIdPid)
        assertEquals("eu.europa.ec.eudi.pid.1", c.pidNamespace)
        assertEquals("min_age", c.paramMinAge)
        assertEquals("nationality_in_set", c.resultNatInSet)
    }

    @Test
    fun predicate_mode_from_token_round_trips() {
        assertEquals(PredicateMode.AGE, predicateModeFromToken("age"))
        assertEquals(PredicateMode.AND, predicateModeFromToken("and"))
        assertNull(predicateModeFromToken("nope"))
    }

    @Test
    fun iso_alpha2_to_numeric_has_full_coverage() {
        assertEquals(300u, isoAlpha2ToNumeric("GR"))
        assertEquals(196u, isoAlpha2ToNumeric("cy")) // case-insensitive
        assertEquals(840u, isoAlpha2ToNumeric("US")) // not just EU
        assertNull(isoAlpha2ToNumeric("ZZ"))
    }

    @Test
    fun prove_then_verify_round_trips() {
        val statement = sampleStatement()
        val proof = proveIdentity(statement, sampleWitness())
        assertTrue(verifyIdentity(statement, proof).ok)
    }

    @Test
    fun verify_rejects_a_tampered_proof() {
        val statement = sampleStatement()
        assertFalse(verifyIdentity(statement, byteArrayOf(0, 0, 0)).ok)
    }

    @Test
    fun verify_rejects_a_proof_for_a_different_statement() {
        val proof = proveIdentity(sampleStatement(ageThreshold = 18u), sampleWitness())
        assertFalse(verifyIdentity(sampleStatement(ageThreshold = 21u), proof).ok)
    }
}
