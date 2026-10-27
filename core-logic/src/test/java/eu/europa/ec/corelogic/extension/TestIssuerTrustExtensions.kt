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

package eu.europa.ec.corelogic.extension

import eu.europa.ec.eudi.etsi1196x2.consultation.CertificationChainValidation
import eu.europa.ec.storagelogic.model.IssuerTrustVerdict
import org.junit.Assert.assertEquals
import org.junit.Test

class TestIssuerTrustExtensions {

    @Test
    fun `Given a trusted chain, When toIssuerTrustVerdict is called, Then TRUSTED is returned`() {
        val result: CertificationChainValidation<String> =
            CertificationChainValidation.Trusted(trustAnchor = "anchor")

        assertEquals(IssuerTrustVerdict.TRUSTED, result.toIssuerTrustVerdict())
    }

    @Test
    fun `Given an untrusted chain, When toIssuerTrustVerdict is called, Then NOT_TRUSTED is returned`() {
        val result: CertificationChainValidation<String> =
            CertificationChainValidation.NotTrusted(cause = IllegalStateException("no anchor"))

        assertEquals(IssuerTrustVerdict.NOT_TRUSTED, result.toIssuerTrustVerdict())
    }

    @Test
    fun `Given no evaluation, When toIssuerTrustVerdict is called, Then NOT_EVALUATED is returned`() {
        val result: CertificationChainValidation<String>? = null

        assertEquals(IssuerTrustVerdict.NOT_EVALUATED, result.toIssuerTrustVerdict())
    }
}