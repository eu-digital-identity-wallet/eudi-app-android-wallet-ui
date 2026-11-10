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

import android.content.Context
import android.content.res.Resources
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.resourceslogic.provider.ResourceProviderImpl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

@RunWith(RobolectricTestRunner::class)
class TestResourceProviderExtensions {

    private val resourceProvider: ResourceProvider =
        ResourceProviderImpl(RuntimeEnvironment.getApplication())

    //region getX509CertificateFromRaw

    // Case 1:
    // 1. The raw resource is the CA that both flavors pass to loteSignerTrustAnchors.
    // Case 1 Expected Result:
    // The CA certificate is returned.
    @Test
    fun `Given Case 1, When getX509CertificateFromRaw is called, Then Case 1 Expected Result is returned`() {
        // When
        val certificate = resourceProvider.getX509CertificateFromRaw(R.raw.pidissuerca02_eu)

        // Then
        assertEquals(
            "C=EU,O=EUDI Wallet Reference Implementation,CN=PID Issuer CA 02",
            certificate.subjectX500Principal.name
        )
        assertEquals(0, certificate.basicConstraints)
    }

    // Case 2:
    // 1. The certificate that signs the development trusted lists, as the lists carry it in x5c.
    // Case 2 Expected Result:
    // Its signature verifies with the anchor's key, so the anchor authenticates that signer.
    @Test
    fun `Given Case 2, When getX509CertificateFromRaw is called, Then Case 2 Expected Result is returned`() {
        // Given
        val signer = javaClass.classLoader!!.getResourceAsStream(DEV_LIST_SIGNER).use {
            CertificateFactory.getInstance("X.509").generateCertificate(it) as X509Certificate
        }

        // When
        val anchor = resourceProvider.getX509CertificateFromRaw(R.raw.pidissuerca02_eu)

        // Then
        assertEquals(anchor.subjectX500Principal, signer.issuerX500Principal)
        signer.verify(anchor.publicKey)
    }

    // Case 3:
    // 1. The raw resource is not a certificate.
    // Case 3 Expected Result:
    // A CertificateException is thrown.
    @Test
    fun `Given Case 3, When getX509CertificateFromRaw is called, Then Case 3 Expected Result is thrown`() {
        // Given
        val resources = mock<Resources> {
            on { openRawResource(NOT_A_CERTIFICATE) } doReturn "not a certificate".byteInputStream()
        }
        val context = mock<Context> { on { this.resources } doReturn resources }
        val provider = mock<ResourceProvider> { on { provideContext() } doReturn context }

        // Then
        assertThrows(CertificateException::class.java) {
            provider.getX509CertificateFromRaw(NOT_A_CERTIFICATE)
        }
    }

    //endregion

    private companion object {
        const val DEV_LIST_SIGNER = "dev_trusted_list_signer.pem"
        const val NOT_A_CERTIFICATE = 1
    }
}
