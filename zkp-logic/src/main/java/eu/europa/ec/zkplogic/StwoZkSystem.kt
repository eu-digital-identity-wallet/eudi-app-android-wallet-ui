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

import com.kss.euid.zk.sdk.ZkPublicStatement
import com.kss.euid.zk.sdk.ZkWitness
import com.kss.euid.zk.sdk.proveIdentity
import com.kss.euid.zk.sdk.verifyIdentity
import com.kss.euid.zk.sdk.zkContractV1
import org.multipaz.cbor.DataItem
import org.multipaz.mdoc.response.MdocDocument
import org.multipaz.mdoc.zkp.ProofVerificationFailureException
import org.multipaz.mdoc.zkp.ZkDocument
import org.multipaz.mdoc.zkp.ZkSystem
import org.multipaz.mdoc.zkp.ZkSystemSpec
import org.multipaz.request.MdocRequestedClaim
import org.multipaz.request.RequestedClaim
import kotlin.time.Instant

/**
 * The shared prover↔verifier contract — system/spec names, the `ZkSystemSpec.params` keys, the EUDI
 * identifiers, and result-claim ids — fetched once from the SDK (the single source of truth). Both the
 * wallet and the verifier read the same values, so neither hardcodes a string the other might change.
 */
internal val ZK_CONTRACT = zkContractV1()

/**
 * STWO-backed [ZkSystem] for the EUDI PID: proves an issuer-signed PID satisfies an age-over-threshold
 * and/or nationality-in-set predicate in zero knowledge, revealing only the boolean outcome.
 *
 * All circuit work is delegated to the Rust SDK (`com.kss.euid.zk.sdk`).
 */
class StwoZkSystem : ZkSystem {

    override val name: String = ZK_CONTRACT.systemName

    /**
     * The ZK system specs this wallet can produce. Advertised via [StwoZkSystem.systemSpecs]; the concrete
     * predicate parameters (`min_age`, `accepted_countries`, …) are supplied per-request by the verifier
     * and read off the matched [ZkSystemSpec] at proving time. Identifiers come from the SDK contract.
     */
    override val systemSpecs: List<ZkSystemSpec> = listOf(
        ZkSystemSpec(
            id = ZK_CONTRACT.specIdPid,
            system = ZK_CONTRACT.systemName,
        ).apply {
            // Circuit identity (mirrors Longfellow's version/num_attributes/circuit_hash convention).
            addParam(ZK_CONTRACT.paramVersion, 1L)
            addParam(ZK_CONTRACT.paramNumAttributes, 2L)
        }
    )

    override fun getMatchingSystemSpec(
        zkSystemSpecs: List<ZkSystemSpec>,
        requestedClaims: List<RequestedClaim>,
    ): ZkSystemSpec? = zkSystemSpecs.firstOrNull { spec ->
        spec.system == name && requestedClaimsSupported(requestedClaims)
    }

    override fun generateProof(
        zkSystemSpec: ZkSystemSpec,
        document: MdocDocument,
        sessionTranscript: DataItem,
        timestamp: Instant,
    ): ZkDocument {
        val statement = ZkPublicStatement.forProver(
            spec = zkSystemSpec,
            document = document,
            sessionTranscript = sessionTranscript,
            timestamp = timestamp
        );
        val witness = ZkWitness.from(document)

        val proof: ByteArray = proveIdentity(statement, witness)

        return ZkDocument.from(zkSystemSpec, document, proof, timestamp)
    }

    override fun verifyProof(
        zkDocument: ZkDocument,
        zkSystemSpec: ZkSystemSpec,
        sessionTranscript: DataItem,
    ) {
        val statement = ZkPublicStatement.forVerifier(
            spec = zkSystemSpec,
            zkDocument = zkDocument,
            sessionTranscript = sessionTranscript
        );
        val result = verifyIdentity(statement = statement, proof = zkDocument.proof.toByteArray())
        if (!result.ok) {
            throw ProofVerificationFailureException("STWO ZK proof verification failed")
        }
    }

    private fun requestedClaimsSupported(requestedClaims: List<RequestedClaim>): Boolean {
        val mdocClaims = requestedClaims.filterIsInstance<MdocRequestedClaim>()
        if (mdocClaims.isEmpty()) return false
        val supported = setOf(ZK_CONTRACT.elementBirthDate, ZK_CONTRACT.elementNationality)
        return mdocClaims.all {
            it.namespaceName == ZK_CONTRACT.pidNamespace && it.dataElementName in supported
        }
    }
}
