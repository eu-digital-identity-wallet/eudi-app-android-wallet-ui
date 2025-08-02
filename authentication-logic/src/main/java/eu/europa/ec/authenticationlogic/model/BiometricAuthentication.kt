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

package eu.europa.ec.authenticationlogic.model

import com.google.gson.annotations.SerializedName

/**
 * Data class used to represent the Biometric information saved in shared preferences and are needed
 * to validate the biometric authentication.
 *
 * @param randomString          The random String that is used for biometric validation.
 * @param encryptedString       The random String encrypted with biometric cipher and in base64 form.
 * @param ivString              The iv used in biometric cipher in base64 form.
 */
data class BiometricAuthentication(
    @SerializedName("random") val randomString: String,
    @SerializedName("encrypted") val encryptedString: String,
    @SerializedName("iv") val ivString: String,
)