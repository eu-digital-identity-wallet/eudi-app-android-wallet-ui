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

import androidx.annotation.RawRes
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

/**
 * The X.509 certificate in the raw resource [resId], in DER or PEM form. Throws when the resource
 * is missing or is not a certificate, so a trust configuration that names it fails when it is
 * built.
 */
internal fun ResourceProvider.getX509CertificateFromRaw(@RawRes resId: Int): X509Certificate =
    provideContext().resources.openRawResource(resId).use {
        CertificateFactory.getInstance("X.509").generateCertificate(it) as X509Certificate
    }
